package com.wizedup.focus.ui.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wizedup.focus.MainActivity
import com.wizedup.focus.WizedUpApplication
import com.wizedup.focus.service.FocusServiceController
import com.wizedup.focus.ui.theme.WizedUpTheme
import com.wizedup.focus.util.FocusDiag
import com.wizedup.focus.util.SystemUiUtils

/**
 * Full-screen lock surface. Manifest declares singleTask + excludeFromRecents +
 * showWhenLocked + turnScreenOn. We additionally:
 *   - apply immersive sticky and KEEP_SCREEN_ON in onCreate;
 *   - override Back to no-op (override onUserLeaveHint is implicit — we do nothing in it);
 *   - start the foreground service if it isn't already running (idempotent);
 *   - finish ourselves when the persisted flag flips to false.
 */
class FocusActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FocusDiag.d("FocusActivity.onCreate")
        SystemUiUtils.applyImmersive(this)

        // Defensive: the activity should only be reachable while focus is active. If we
        // were launched while the flag is false, ensure the foreground service is up so
        // the boot-resume path is consistent. The service self-stops if the flag is false.
        FocusServiceController.start(this)

        // Override system back: no-op. (US-R1-06 AC.)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Intentionally empty. Voluntary exit goes through the Exit Focus button
                    // and confirmation dialog (US-R1-10).
                }
            },
        )

        setContent {
            WizedUpTheme {
                val app = applicationContext as WizedUpApplication
                val vm: FocusViewModel = viewModel {
                    FocusViewModel(
                        focusStateRepository = app.focusStateRepository,
                        studentProfileRepository = app.studentProfileRepository,
                    )
                }

                val state by vm.state.collectAsState()
                val profile by vm.profile.collectAsState()
                val elapsed by vm.elapsedMs.collectAsState()

                // [FocusViewModel.state] uses stateIn(initialValue = Inactive). The first
                // composition therefore sees isActive=false *before* DataStore has emitted,
                // which used to trigger this block and stop the service while the persisted
                // flag was still true — MainActivity then relaunched us in an infinite loop.
                // Only tear down after we have observed at least one active=true from the repo.
                val hasSeenActiveFocus = remember { mutableStateOf(false) }

                // Reactive finish: when the persisted flag flips false (voluntary exit, or
                // an external write), we tear down the service and finish.
                LaunchedEffect(state.isActive) {
                    FocusDiag.d("FocusActivity LaunchedEffect isActive=${state.isActive} hasSeenActive=${hasSeenActiveFocus.value}")
                    if (state.isActive) {
                        hasSeenActiveFocus.value = true
                        return@LaunchedEffect
                    }
                    if (!hasSeenActiveFocus.value) {
                        FocusDiag.d("FocusActivity skip teardown (awaiting first real active emission; not a deactivate)")
                        return@LaunchedEffect
                    }
                    FocusDiag.d("FocusActivity -> deactivate path: stop service, MainActivity, finish()")
                    FocusServiceController.stop(this@FocusActivity)
                    // Land on MainActivity (home, green state).
                    startActivity(
                        Intent(this@FocusActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        },
                    )
                    finish()
                }

                FocusScreen(
                    displayName = profile?.displayName.orEmpty(),
                    elapsedMs = elapsed,
                    onExitConfirmed = vm::deactivate,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FocusDiag.d("FocusActivity.onResume")
    }

    override fun onPause() {
        FocusDiag.d("FocusActivity.onPause")
        super.onPause()
    }

    /**
     * `onUserLeaveHint` fires when the user presses Home or invokes a system action that
     * sends the activity to the background. We deliberately do nothing — the AccessibilityService
     * brings us back via the relaunch loop in [com.wizedup.focus.service.FocusAccessibilityService].
     */
    override fun onUserLeaveHint() {
        // No-op by design.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FocusDiag.d("FocusActivity.onWindowFocusChanged hasFocus=$hasFocus")
        SystemUiUtils.reapplyImmersiveOnFocusChange(this, hasFocus)
    }
}
