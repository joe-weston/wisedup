package com.wisedup.focus.ui.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wisedup.focus.MainActivity
import com.wisedup.focus.WisedUpApplication
import com.wisedup.focus.service.FocusServiceController
import com.wisedup.focus.ui.theme.WisedUpTheme
import com.wisedup.focus.util.SystemUiUtils

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
            WisedUpTheme {
                val app = applicationContext as WisedUpApplication
                val vm: FocusViewModel = viewModel {
                    FocusViewModel(
                        focusStateRepository = app.focusStateRepository,
                        studentProfileRepository = app.studentProfileRepository,
                    )
                }

                val state by vm.state.collectAsState()
                val profile by vm.profile.collectAsState()
                val elapsed by vm.elapsedMs.collectAsState()

                // Reactive finish: when the persisted flag flips false (voluntary exit, or
                // an external write), we tear down the service and finish.
                LaunchedEffect(state.isActive) {
                    if (!state.isActive) {
                        FocusServiceController.stop(this@FocusActivity)
                        // Land on MainActivity (home, green state).
                        startActivity(
                            Intent(this@FocusActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            },
                        )
                        finish()
                    }
                }

                FocusScreen(
                    displayName = profile?.displayName.orEmpty(),
                    elapsedMs = elapsed,
                    onExitConfirmed = vm::deactivate,
                )
            }
        }
    }

    /**
     * `onUserLeaveHint` fires when the user presses Home or invokes a system action that
     * sends the activity to the background. We deliberately do nothing — the AccessibilityService
     * brings us back via the relaunch loop in [com.wisedup.focus.service.FocusAccessibilityService].
     */
    override fun onUserLeaveHint() {
        // No-op by design.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SystemUiUtils.reapplyImmersiveOnFocusChange(this, hasFocus)
    }
}
