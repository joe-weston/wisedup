package com.wisedup.focus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.wisedup.focus.data.FocusStateRepository
import com.wisedup.focus.data.StudentProfileRepository
import com.wisedup.focus.data.wisedupDataStore

/**
 * Application entry point.
 *
 * Manual DI per the engineering brief — Hilt is intentionally NOT used in R1 to keep the
 * surface area small. R2 may refactor to Hilt when the network/sync layer multiplies the
 * graph. For now the two repositories are exposed as `lateinit` singletons accessible
 * via [WisedUpApplication.instance] from non-DI components (services, receivers).
 */
class WisedUpApplication : Application() {

    lateinit var focusStateRepository: FocusStateRepository
        private set

    lateinit var studentProfileRepository: StudentProfileRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Wire the DataStore-backed repositories. The DataStore singleton itself is
        // declared as a Context extension; this is the first read, so the file is
        // lazily created on first call.
        focusStateRepository = FocusStateRepository(wisedupDataStore)
        studentProfileRepository = StudentProfileRepository(wisedupDataStore)

        createNotificationChannel()
    }

    /**
     * Creates the high-importance notification channel used by [FocusForegroundService].
     * Idempotent — calling [NotificationManager.createNotificationChannel] with the same
     * channel ID is a no-op after the first call.
     *
     * Per US-R1-09 ACs. The architecture spec calls for IMPORTANCE_HIGH; the engineering
     * brief asks for IMPORTANCE_LOW. We honor the engineering brief: IMPORTANCE_LOW makes
     * the notification non-intrusive (no heads-up, no sound) which is correct for an
     * always-on status indicator. The channel name and description match US-R1-09.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID_FOCUS_MODE,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID_FOCUS_MODE = "focus_mode"

        @Volatile
        private var instance: WisedUpApplication? = null

        /**
         * Used by services and receivers that aren't part of the Compose graph.
         * Will throw if called before [onCreate]; that should never happen in practice.
         */
        fun get(): WisedUpApplication =
            instance ?: error("WisedUpApplication.onCreate has not run yet")
    }
}
