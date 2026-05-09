package com.wisedup.focus.service

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
    fun startIntent(context: Context): Intent =
        Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_START
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
    fun start(context: Context) {
        ContextCompat.startForegroundService(context, startIntent(context))
    }

    /**
     * Ask the service to tear down its notification and stopSelf.
     * We send an intent rather than calling stopService directly so that the service
     * can run any cleanup before exit.
     */
    fun stop(context: Context) {
        // Best-effort: try the explicit stop intent first; if the service is dead,
        // this is a no-op. Either way, also call stopService for guaranteed teardown.
        context.startService(stopIntent(context))
        context.stopService(Intent(context, FocusForegroundService::class.java))
    }
}
