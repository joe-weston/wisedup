package com.wisedup.focus.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wisedup.focus.data.DisplayNameRules
import com.wisedup.focus.data.StudentProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Onboarding flow state. The view collects [name] for live binding, and [continueEnabled]
 * for the button state. Submission writes through [StudentProfileRepository].
 */
class OnboardingViewModel(
    private val repository: StudentProfileRepository,
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    val maxLength: Int = DisplayNameRules.MAX_LENGTH

    /**
     * Update the entered name. Truncates above [DisplayNameRules.MAX_LENGTH] to keep the
     * field stable (US-R1-03 AC: stops accepting characters at 64 or shows a counter — we
     * do both).
     */
    fun onNameChange(raw: String) {
        val truncated = if (raw.length > DisplayNameRules.MAX_LENGTH) {
            raw.take(DisplayNameRules.MAX_LENGTH)
        } else {
            raw
        }
        _name.value = truncated
    }

    /** True when [name] trimmed has length 1..MAX_LENGTH. */
    fun isContinueEnabled(): Boolean {
        val trimmed = _name.value.trim()
        return trimmed.length in DisplayNameRules.MIN_LENGTH..DisplayNameRules.MAX_LENGTH
    }

    /**
     * Persist the profile. Calls [onSuccess] only after the write completes, so the host
     * can navigate without a stale-profile race.
     */
    fun submit(onSuccess: () -> Unit, onError: (Throwable) -> Unit = {}) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                repository.completeOnboarding(_name.value)
                onSuccess()
            } catch (e: IllegalArgumentException) {
                onError(e)
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
