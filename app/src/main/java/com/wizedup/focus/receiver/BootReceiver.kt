package com.wizedup.focus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wizedup.focus.WizedUpApplication
import com.wizedup.focus.service.FocusServiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Re-arms Focus Mode after a reboot per ADR-003 §Decision and US-R1-11 ACs.
 *
 * **LOCKED_BOOT_COMPLETED handling.** The manifest filter accepts both BOOT_COMPLETED and
 * LOCKED_BOOT_COMPLETED so that we receive *something* on direct-boot-aware OEMs that
 * deliver the locked broadcast first. However, DataStore is *not* direct-boot aware:
 * its credential-protected storage isn't readable until the user unlocks. So when we
 * receive LOCKED_BOOT_COMPLETED we deliberately no-op and wait for the regular
 * BOOT_COMPLETED to arrive after user unlock. (Receiver itself sets directBootAware=false,
 * but the broadcast can still be queued; the early no-op is defensive.)
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
            // LOCKED_BOOT_COMPLETED: deliberately ignored — DataStore is not direct-boot aware.
            // We will be re-delivered BOOT_COMPLETED after the user unlocks.
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> Unit
        }
    }

    private fun handleBootCompleted(context: Context) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        scope.launch {
            try {
                // Bounded read: ADR-003 says ≤ 10 s. If DataStore is slow (rare; happens on
                // some restored devices), we err on the side of doing nothing rather than
                // hanging the receiver and risking an ANR.
                val state = withTimeoutOrNull(BOOT_READ_TIMEOUT_MS) {
                    WizedUpApplication.get().focusStateRepository.snapshot(BOOT_READ_TIMEOUT_MS)
                }

                if (state?.isActive == true) {
                    // Per ADR-003: start the foreground service. The accessibility service
                    // will rebind on its own schedule; the persistent notification is the
                    // bridge. Per locked PM decision #3, no toast / no banner.
                    FocusServiceController.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val BOOT_READ_TIMEOUT_MS = 10_000L
    }
}
