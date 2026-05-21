package com.wizedup.focus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wizedup.focus.data.FocusStateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home-screen state. Exposes the live focus flag from [FocusStateRepository] for the UI
 * to react when an external write flips it (e.g. boot resume), and an `activate()` helper
 * that writes the persisted flag.
 *
 * The activate() side effect of *starting the foreground service and launching
 * FocusActivity* lives at the activity boundary — see MainActivity / FocusActivity. The
 * VM only owns the persisted-flag mutation. This split keeps the VM testable without
 * needing a Context.
 */
class HomeViewModel(
    private val focusStateRepository: FocusStateRepository,
) : ViewModel() {

    val isFocusActive: StateFlow<Boolean> = focusStateRepository.isActive.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = false,
    )

    /**
     * Tap-counting note (US-R1-04 + engineering brief):
     * The home-screen "Activate Focus" button is **tap #1** from the user's perspective on
     * the second-and-subsequent app launches. If accessibility is already granted and
     * notifications are allowed, calling [activate] writes the flag and the host activity
     * launches FocusActivity directly — there is no second in-app tap. So second-launch
     * tap-to-active = 1 (just this button). On the very first launch, the path also includes
     * a one-time accessibility grant in system Settings, which is a system-side action and
     * does NOT count as an in-app tap per the brief. Total in-app taps to active focus
     * remain ≤ 2 in all cases.
     */
    fun activate(onActivated: () -> Unit) {
        viewModelScope.launch {
            focusStateRepository.activate()
            onActivated()
        }
    }
}
