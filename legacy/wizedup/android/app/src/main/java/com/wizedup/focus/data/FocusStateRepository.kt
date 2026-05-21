package com.wizedup.focus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.UUID

/**
 * App-wide DataStore singleton. Filename matches ADR-004 §"Schema" exactly so that
 * `data_extraction_rules.xml` and `backup_rules.xml` can target it.
 */
internal val Context.wizedupDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wizedup_state",
)

/** Immutable focus-state snapshot, used by UI, services, and the boot receiver. */
data class FocusState(
    val isActive: Boolean,
    val startedAtMs: Long?,
) {
    companion object {
        val Inactive = FocusState(isActive = false, startedAtMs = null)
    }
}

/** Immutable student-profile snapshot. Null until onboarding has completed. */
data class StudentProfile(
    val id: String,
    val displayName: String,
    val createdAtMs: Long,
)

/**
 * Single source of truth for focus-mode state. Backed by DataStore Preferences.
 *
 * All reads are async via [Flow]. The one exception is [snapshot], which is intended
 * for `BroadcastReceiver.goAsync()` callers that need a bounded-blocking read.
 */
class FocusStateRepository(
    private val dataStore: DataStore<Preferences>,
) {
    /** Cold flow of the current focus state. Default = Inactive on a fresh install. */
    val state: Flow<FocusState> = dataStore.data
        .map { prefs ->
            val isActive = prefs[PreferencesKeys.FOCUS_IS_ACTIVE] ?: false
            val startedAt = prefs[PreferencesKeys.FOCUS_STARTED_AT_MS] ?: 0L
            FocusState(
                isActive = isActive,
                startedAtMs = if (startedAt > 0L) startedAt else null,
            )
        }
        .distinctUntilChanged()

    /** Convenience flow for UI that only cares about the boolean. */
    val isActive: Flow<Boolean> = state.map { it.isActive }.distinctUntilChanged()

    /** Sets is_active=true and started_at_ms=now. */
    suspend fun activate(nowMs: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_IS_ACTIVE] = true
            prefs[PreferencesKeys.FOCUS_STARTED_AT_MS] = nowMs
        }
    }

    /** Sets is_active=false and clears started_at_ms (sentinel 0L). */
    suspend fun deactivate() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_IS_ACTIVE] = false
            prefs[PreferencesKeys.FOCUS_STARTED_AT_MS] = 0L
        }
    }

    /**
     * Synchronous-style read for [android.content.BroadcastReceiver.goAsync] callers.
     * Bounded at 10 s per ADR-003 §Decision.
     *
     * Do NOT call this from the main thread of an Activity — use the [state] flow there.
     */
    fun snapshot(timeoutMs: Long = 10_000L): FocusState = runBlocking {
        withTimeout(timeoutMs) { state.first() }
    }
}

/**
 * Single source of truth for the student profile. Profile is `null` until onboarding
 * completes (US-R1-03). Display-name validation rejects empty / over-length input
 * before any write happens (US-R1-02 ACs).
 */
class StudentProfileRepository(
    private val dataStore: DataStore<Preferences>,
) {
    /** Null = not onboarded. Non-null = onboarded with a valid profile. */
    val profile: Flow<StudentProfile?> = dataStore.data
        .map { prefs ->
            val id = prefs[PreferencesKeys.STUDENT_ID]
            val name = prefs[PreferencesKeys.STUDENT_DISPLAY_NAME]
            val createdAt = prefs[PreferencesKeys.STUDENT_CREATED_AT_MS]

            if (id.isNullOrBlank() || name.isNullOrBlank() || createdAt == null) return@map null

            // Validate UUID; if corrupt (manual file edit), treat as not-onboarded.
            // Per ADR-004 §Validation Rules.
            val validId = runCatching { UUID.fromString(id).toString() }.getOrNull() ?: return@map null

            StudentProfile(id = validId, displayName = name, createdAtMs = createdAt)
        }
        .distinctUntilChanged()

    /** True if a valid profile exists. */
    suspend fun isOnboarded(): Boolean = profile.first() != null

    /**
     * Writes a fresh profile with a UUIDv4 id. Throws [IllegalArgumentException] if the
     * trimmed display name is empty or longer than [DisplayNameRules.MAX_LENGTH].
     *
     * Idempotent for repeated calls with the same name (same UUID is preserved on subsequent
     * calls — see test). In R1 we never expect this to be called twice; if a future story
     * needs profile editing it should add a separate `updateDisplayName` method.
     */
    suspend fun completeOnboarding(
        displayName: String,
        nowMs: Long = System.currentTimeMillis(),
        idGenerator: () -> String = { UUID.randomUUID().toString() },
    ): StudentProfile {
        val validated = DisplayNameRules.validate(displayName)
        var resultId = ""
        var resultCreatedAt = 0L

        dataStore.edit { prefs ->
            val existingId = prefs[PreferencesKeys.STUDENT_ID]
            val isExistingValid = existingId != null &&
                runCatching { UUID.fromString(existingId) }.isSuccess

            val finalId = if (isExistingValid) existingId!! else idGenerator()
            val finalCreatedAt = prefs[PreferencesKeys.STUDENT_CREATED_AT_MS] ?: nowMs

            prefs[PreferencesKeys.STUDENT_ID] = finalId
            prefs[PreferencesKeys.STUDENT_DISPLAY_NAME] = validated
            prefs[PreferencesKeys.STUDENT_CREATED_AT_MS] = finalCreatedAt

            resultId = finalId
            resultCreatedAt = finalCreatedAt
        }

        return StudentProfile(id = resultId, displayName = validated, createdAtMs = resultCreatedAt)
    }
}
