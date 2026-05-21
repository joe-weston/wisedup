package com.wizedup.focus.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Typed keys for the WizedUp DataStore Preferences file (`wizedup_state.preferences_pb`).
 * See ADR-004 for the full schema rationale.
 *
 * Two namespaces:
 *  - student.*  written once during onboarding; never deleted on the happy path.
 *    R2 adds `student.school_id` + `student.sync_token` after school registration (ADR-005).
 *  - focus.*    toggled by the user; read by every component (UI, service, receiver).
 *    R2 adds `focus.client_session_id` while a session is active.
 */
internal object PreferencesKeys {
    // student.*
    val STUDENT_ID = stringPreferencesKey("student.id")
    val STUDENT_DISPLAY_NAME = stringPreferencesKey("student.display_name")
    val STUDENT_CREATED_AT_MS = longPreferencesKey("student.created_at_ms")
    val STUDENT_SCHOOL_ID = stringPreferencesKey("student.school_id")
    val STUDENT_SYNC_TOKEN = stringPreferencesKey("student.sync_token")

    // focus.*
    val FOCUS_IS_ACTIVE = booleanPreferencesKey("focus.is_active")
    val FOCUS_STARTED_AT_MS = longPreferencesKey("focus.started_at_ms")
    val FOCUS_CLIENT_SESSION_ID = stringPreferencesKey("focus.client_session_id")
}

/** Display name validation. Trim then check 1..MAX_LENGTH. */
internal object DisplayNameRules {
    const val MAX_LENGTH = 64
    const val MIN_LENGTH = 1

    fun validate(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.length in MIN_LENGTH..MAX_LENGTH) {
            "display_name length must be in $MIN_LENGTH..$MAX_LENGTH after trim (was ${trimmed.length})"
        }
        return trimmed
    }
}

/** School join code typed at R2 registration (opaque string from the school). */
object SchoolCodeRules {
    const val MAX_LENGTH = 32
    const val MIN_LENGTH = 4

    private val allowed = Regex("^[A-Za-z0-9-]+$")

    fun validate(raw: String): String {
        val trimmed = raw.trim().uppercase()
        require(trimmed.length in MIN_LENGTH..MAX_LENGTH) {
            "school_code length must be in $MIN_LENGTH..$MAX_LENGTH after trim (was ${trimmed.length})"
        }
        require(allowed.matches(trimmed)) {
            "school_code may only contain letters, digits, and hyphen"
        }
        return trimmed
    }
}
