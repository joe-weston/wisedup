package com.wizedup.focus.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Helpers for the FocusActivity fullscreen / immersive treatment.
 * See US-R1-06 AC: status/nav bar hidden or color-matched, no system bar contrast.
 */
object SystemUiUtils {

    /**
     * Apply immersive sticky behavior + edge-to-edge content. Hides system bars and
     * lets a swipe from the edge reveal them transiently — they auto-hide again.
     *
     * Also sets FLAG_KEEP_SCREEN_ON so the focus screen does not blank during long sessions.
     */
    fun applyImmersive(activity: Activity) {
        // Edge-to-edge content under the system bars (so our red/amber surface reaches them).
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // Keep screen on while focus is active. Without this the screen blanks per the
        // user's display-timeout setting and the lock surface is no longer the user's view.
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Showing this activity above the keyguard. The manifest also declares showWhenLocked;
        // the programmatic call is belt-and-suspenders for older OEM keyguards.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    /** Best-effort UI flag re-application after configuration changes (rotation). */
    fun reapplyImmersiveOnFocusChange(activity: Activity, hasFocus: Boolean) {
        if (!hasFocus) return
        val decor = activity.window.decorView
        // Touch the decor view to nudge insets re-application. Compose handles the rest.
        decor.requestLayout()
    }
}
