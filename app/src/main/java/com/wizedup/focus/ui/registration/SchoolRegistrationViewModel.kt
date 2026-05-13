package com.wizedup.focus.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wizedup.focus.data.SchoolCodeRules
import com.wizedup.focus.data.SchoolRegistrationRepository
import com.wizedup.focus.data.StudentProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SchoolRegistrationViewModel(
    private val schoolRepo: SchoolRegistrationRepository,
    private val studentRepo: StudentProfileRepository,
) : ViewModel() {

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    val maxLength: Int = SchoolCodeRules.MAX_LENGTH

    fun onCodeChange(raw: String) {
        val filtered = raw.uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' || it == '-' }
            .take(SchoolCodeRules.MAX_LENGTH)
        _code.value = filtered
    }

    fun isContinueEnabled(): Boolean {
        val c = _code.value.trim()
        return c.length in SchoolCodeRules.MIN_LENGTH..SchoolCodeRules.MAX_LENGTH
    }

    fun submit(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val profile = studentRepo.profile.first()
                if (profile == null) {
                    onError("Profile missing")
                    return@launch
                }
                schoolRepo.registerWithSchoolCode(_code.value, profile.id, profile.displayName)
                onSuccess()
            } catch (e: IllegalArgumentException) {
                onError(e.message ?: "Invalid school code")
            } catch (e: Exception) {
                onError(e.message ?: "Could not reach school server")
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
