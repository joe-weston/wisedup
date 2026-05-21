package com.wizedup.focus.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Thin start/stop helpers for [FocusForegroundService]. Wraps
 * [ContextCompat.startForegroundService] so callers don't have to remember the
 * Android-O-or-higher gymnastics, and exposes a single point of test seamage.
 *
 * This is *not* the FocusController described in the architecture doc — that pure-logic
 * activate/deactivate orchestrator lives implicitly in the ViewModels for R1 (we kept the
 * surface small per the engineering brief). This object covers the Service-side concerns.
 */
object FocusServiceController {

    /** Build the Intent that starts the foreground service. Public for testability. */
    fun startIntent(context: Context, fromBootResume: Boolean = false): Intent =
        Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_START
            if (fromBootResume) {
                putExtra(FocusForegroundService.EXTRA_FROM_BOOT_RESUME, true)
            }
        }

    /** Build the Intent that asks the service to stop itself. */
    fun stopIntent(context: Context): Intent =
        Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_STOP
        }

    /**
     * Start the foreground service, honoring Android-O+ rules.
     * Safe to call repeatedly — the service is single-instance.
     */
    fun start(context: Context, fromBootResume: Boolean = false) {
        ContextCompat.startForegroundService(context, startIntent(context, fromBootResume))
    }

    /**
     * Ask the service to tear down its notification and stopSelf.
     * We send an intent rather than calling stopService directly so that the service
     * can run any cleanup before exit.
     */
    fun stop(context: Context) {
        // Single-path teardown: stopService triggers onDestroy, which already cancels the
        // notification and any in-flight cleanup. Avoids the prior dual start+stop, which
        // could race against a freshly-bound foreground notification on slow devices.
        context.stopService(Intent(context, FocusForegroundService::class.java))
    }
}
