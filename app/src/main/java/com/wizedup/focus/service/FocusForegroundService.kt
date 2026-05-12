package com.wizedup.focus.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wizedup.focus.R
import com.wizedup.focus.WizedUpApplication
import com.wizedup.focus.ui.focus.FocusActivity
import com.wizedup.focus.util.FocusDiag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service whose only purpose is to hold a persistent notification while
 * Focus Mode is active.
 *
 * - Type: `specialUse` (Android 14+). Subtype "focus_lock_persistence" is declared via
 *   `<property>` in the manifest.
 * - START_STICKY so the OS restarts us on memory-pressure kill (US-R1-07 AC).
 * - On restart we re-read [com.wizedup.focus.data.FocusStateRepository] and self-stop if
 *   focus is no longer active (defensive — covers the case where the service is restarted
 *   after a deactivate write that the OS missed).
 * - Notification channel is created in [WizedUpApplication.onCreate]; we just build into it.
 *
 * See ADR-002 §Decision and ADR-003 §Decision.
 */
class FocusForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat(buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FocusDiag.d("FGS onStartCommand action=${intent?.action} flags=$flags startId=$startId")
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
            else -> {
                // ACTION_START or null (system restart) — ensure the notification is up,
                // then verify state. If state says inactive, self-stop.
                startForegroundCompat(buildNotification())
                verifyStateOrStop()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * If the OS restarts us after a memory kill but the user has since deactivated focus,
     * self-stop. This is a defensive read of the persisted flag.
     */
    private fun verifyStateOrStop(): Job = scope.launch {
        val isActive = WizedUpApplication.get()
            .focusStateRepository
            .isActive
            .first()
        FocusDiag.d("FGS verifyStateOrStop isActive=$isActive")
        if (!isActive) {
            FocusDiag.d("FGS verifyStateOrStop -> stopForegroundAndSelf")
            stopForegroundAndSelf()
        }
    }

    private fun stopForegroundAndSelf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    /**
     * Wrap the API-34 startForeground signature behind a single helper.
     * On 34+, the FGS type must match the manifest declaration (specialUse).
     */
    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, FocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val pi = PendingIntent.getActivity(
            this,
            REQUEST_CODE_TAP,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, WizedUpApplication.CHANNEL_ID_FOCUS_MODE)
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(getColor(R.color.notification_icon_tint))
            .setContentIntent(pi)
            .build()
    }

    companion object {
        const val ACTION_START = "com.wizedup.focus.action.START_FOCUS"
        const val ACTION_STOP = "com.wizedup.focus.action.STOP_FOCUS"

        // Stable id; the OS replaces a notification with the same id when we update.
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_TAP = 100
    }
}
