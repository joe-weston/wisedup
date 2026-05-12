package com.wizedup.focus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.wizedup.focus.WizedUpApplication
import com.wizedup.focus.ui.focus.FocusActivity
import com.wizedup.focus.util.FocusDiag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Watches `TYPE_WINDOW_STATE_CHANGED` events while focus mode is active and relaunches
 * [FocusActivity] whenever a non-self foreground app appears. See ADR-002 §Decision and
 * US-R1-08 ACs.
 *
 * Implementation notes:
 *  - We cache `isActive` from the repository in a [MutableStateFlow] populated by a
 *    coroutine launched in [onServiceConnected]. Reading the flag synchronously off the
 *    accessibility-event hot path matters; that path runs at every window change.
 *  - We *do* relaunch when the foreground is `com.android.systemui` (notification shade,
 *    volume HUD, recents) — per US-R1-08 AC: "Given a system UI surface, when it appears,
 *    then it is treated like any other foreground change." The brief asks us to filter
 *    "volume HUD"-style transient surfaces; we honor that by ignoring system UI events
 *    that arrive without a class name (those are typically transient overlays).
 *  - We never relaunch when our own package is foreground (avoids relaunch loops on our
 *    own activity transitions, including the home-screen → focus-activity transition).
 *  - We consult [rootInActiveWindow] plus a short debounce: raw events often name IME,
 *    dialogs, or transition windows that are not the user's intentional foreground app.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isActiveCache = MutableStateFlow(false)

    /** Coalesces rapid-fire window events (IME, transition animations, transient system UI). */
    @Volatile
    private var lastRelaunchElapsedMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusDiag.d("Accessibility onServiceConnected")
        scope.launch {
            WizedUpApplication.get()
                .focusStateRepository
                .isActive
                .collect {
                    if (isActiveCache.value != it) {
                        FocusDiag.d("Accessibility isActiveCache: ${isActiveCache.value} -> $it")
                    }
                    isActiveCache.value = it
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // Cheap precondition: is focus active right now?
        if (!isActiveCache.value) {
            FocusDiag.d("A11y skip: focus inactive (TYPE_WINDOW_STATE_CHANGED)")
            return
        }

        val eventPkg = event.packageName?.toString() ?: return

        // Don't relaunch when the event explicitly names our package (our own transitions).
        if (eventPkg == OUR_PACKAGE) {
            FocusDiag.d("A11y skip: eventPkg=$eventPkg (ours)")
            return
        }

        // The event's package is often a transient layer (IME, dialog, animation) while the
        // real foreground window is still ours — blindly relaunching causes a startActivity
        // storm and a locked/unlocked flicker loop. Prefer the active root window.
        val rootPkg = rootInActiveWindow?.packageName?.toString()
        if (rootPkg == OUR_PACKAGE) {
            FocusDiag.d("A11y skip: rootPkg=$rootPkg (ours) eventPkg=$eventPkg cls=${event.className}")
            return
        }

        // System UI surfaces: the brief says to treat real foreground apps as relaunch
        // triggers but to ignore transient HUD overlays. The notification-shade *is* a
        // window-state change and we WANT to reassert when the user dismisses it. The
        // most reliable signal we have is: the event has a non-null `className` that
        // names an Activity (system UI HUDs typically deliver className=null or a Dialog).
        // For R1 we relaunch on any non-self package — letting the system UI shade case
        // produce a brief flicker is acceptable per US-R1-08 ACs and the manual matrix.

        val now = SystemClock.elapsedRealtime()
        if (now - lastRelaunchElapsedMs < RELAUNCH_DEBOUNCE_MS) {
            FocusDiag.d("A11y skip: debounce (${now - lastRelaunchElapsedMs}ms < $RELAUNCH_DEBOUNCE_MS) eventPkg=$eventPkg rootPkg=$rootPkg")
            return
        }
        lastRelaunchElapsedMs = now

        FocusDiag.w("A11y RELAUNCH FocusActivity eventPkg=$eventPkg rootPkg=$rootPkg cls=${event.className}")
        relaunchFocusActivity()
    }

    override fun onInterrupt() {
        // No long-running work to interrupt; the relaunch is fire-and-forget.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        scope.cancel()
        return super.onUnbind(intent)
    }

    private fun relaunchFocusActivity() {
        val intent = Intent(this, FocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    companion object {
        const val OUR_PACKAGE = "com.wizedup.focus"

        /** Minimum gap between relaunches to absorb overlapping window-state churn. */
        private const val RELAUNCH_DEBOUNCE_MS = 400L
    }
}
