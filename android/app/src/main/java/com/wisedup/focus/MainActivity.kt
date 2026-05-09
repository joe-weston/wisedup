package com.wisedup.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wisedup.focus.data.StudentProfile
import com.wisedup.focus.ui.focus.FocusActivity
import com.wisedup.focus.ui.home.HomeScreen
import com.wisedup.focus.ui.home.HomeViewModel
import com.wisedup.focus.ui.onboarding.OnboardingScreen
import com.wisedup.focus.ui.onboarding.OnboardingViewModel
import com.wisedup.focus.ui.theme.WisedUpTheme

/**
 * Single-activity host. Routes to OnboardingScreen if no profile, otherwise HomeScreen.
 * If the persisted focus flag is `true` on entry, we relaunch [FocusActivity] so a user
 * who somehow lands on the home screen during an active session is bounced back to the
 * lock surface. (Normal active-session paths never go through MainActivity.)
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WisedUpTheme {
                AppRoot(
                    onActivateFocus = ::launchFocusActivity,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If focus is already active on resume, jump straight to FocusActivity.
        // We don't block onCreate on this read — the flow-driven check in AppRoot also
        // fires, and this guards against the race where the user backgrounds during
        // onCreate composition.
        val isActive = WisedUpApplication.get().focusStateRepository.let { repo ->
            // Best-effort sync read: snapshot is bounded to 10s; here we use a small budget.
            runCatching { repo.snapshot(timeoutMs = 500L) }.getOrNull()?.isActive ?: false
        }
        if (isActive) launchFocusActivity()
    }

    private fun launchFocusActivity() {
        startActivity(
            Intent(this, FocusActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            },
        )
    }
}

@Composable
private fun AppRoot(
    onActivateFocus: () -> Unit,
) {
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext) as WisedUpApplication
    val profileFlow = app.studentProfileRepository.profile
    val profileState: StudentProfile? by profileFlow.collectAsState(initial = null)

    // Reactive jump to FocusActivity if the flag flips on.
    val isActive: Boolean by app.focusStateRepository.isActive.collectAsState(initial = false)
    LaunchedEffect(isActive) {
        if (isActive) onActivateFocus()
    }

    if (profileState == null) {
        OnboardingScreen(viewModel = viewModel { OnboardingViewModel(app.studentProfileRepository) })
    } else {
        HomeScreen(
            profile = profileState!!,
            onActivateFocus = onActivateFocus,
            viewModel = viewModel {
                HomeViewModel(
                    focusStateRepository = app.focusStateRepository,
                )
            },
        )
    }
}
