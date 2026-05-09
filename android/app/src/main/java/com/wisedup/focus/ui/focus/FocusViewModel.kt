package com.wisedup.focus.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wisedup.focus.data.FocusState
import com.wisedup.focus.data.FocusStateRepository
import com.wisedup.focus.data.StudentProfile
import com.wisedup.focus.data.StudentProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the FocusActivity.
 *
 * - [state] mirrors the persisted focus flag + start time.
 * - [profile] gives us the display name to show.
 * - [elapsedMs] ticks once a second while the activity is alive.
 * - [requestExit] tells the host activity to deactivate (after the confirm dialog).
 */
class FocusViewModel(
    private val focusStateRepository: FocusStateRepository,
    studentProfileRepository: StudentProfileRepository,
) : ViewModel() {

    val state: StateFlow<FocusState> = focusStateRepository.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FocusState.Inactive,
    )

    val profile: StateFlow<StudentProfile?> = studentProfileRepository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = null,
    )

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    /**
     * Combined flag for the host activity: when state.isActive flips false, the host
     * should call finish().
     */
    val shouldFinish: StateFlow<Boolean> = combine(state) { values -> !values[0].isActive }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

    private var tickJob: Job? = null

    init {
        // Tick the elapsed counter once per second whenever a session is active.
        viewModelScope.launch {
            state.collect { focusState ->
                tickJob?.cancel()
                if (focusState.isActive && focusState.startedAtMs != null) {
                    tickJob = viewModelScope.launch {
                        while (true) {
                            val started = focusState.startedAtMs
                            _elapsedMs.value = (System.currentTimeMillis() - started)
                                .coerceAtLeast(0L)
                            delay(1_000L)
                        }
                    }
                } else {
                    _elapsedMs.value = 0L
                }
            }
        }
    }

    /** Persist the deactivate. The host activity finishes itself when state flips. */
    fun deactivate() {
        viewModelScope.launch { focusStateRepository.deactivate() }
    }
}
