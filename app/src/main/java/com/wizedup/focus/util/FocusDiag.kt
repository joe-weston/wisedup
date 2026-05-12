package com.wizedup.focus.util

import android.util.Log
import com.wizedup.focus.BuildConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 * Focus-mode diagnostics for Logcat. Logs are suppressed unless:
 *  - this is a **debug** build ([BuildConfig.DEBUG]), or
 *  - you enable the tag at runtime: `adb shell setprop log.tag.FocusDiag DEBUG`
 *
 * **Filter Logcat (noise-free):**
 * ```
 * adb logcat -s FocusDiag:D
 * ```
 *
 * Lines are prefixed with a monotonic `#n` so you can correlate bursts when the UI
 * “rotates” or the accessibility service fires overlapping window events.
 */
object FocusDiag {
    const val TAG = "FocusDiag"

    private val seq = AtomicInteger(0)

    private fun enabled(): Boolean =
        BuildConfig.DEBUG || Log.isLoggable(TAG, Log.DEBUG)

    fun d(message: String) {
        if (!enabled()) return
        Log.d(TAG, "[#${seq.incrementAndGet()}] $message")
    }

    fun w(message: String) {
        if (!enabled()) return
        Log.w(TAG, "[#${seq.incrementAndGet()}] $message")
    }
}
