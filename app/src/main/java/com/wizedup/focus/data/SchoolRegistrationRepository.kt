package com.wizedup.focus.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.wizedup.focus.data.remote.SupabaseRestClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * R2 school association + sync token (ADR-005). Registration is a direct RPC, not outboxed.
 */
class SchoolRegistrationRepository(
    private val appContext: Context,
    private val dataStore: DataStore<Preferences>,
    private val supabase: SupabaseRestClient?,
) {

    val isRegistered: Flow<Boolean> = dataStore.data
        .map { prefs ->
            val school = prefs[PreferencesKeys.STUDENT_SCHOOL_ID]
            val token = prefs[PreferencesKeys.STUDENT_SYNC_TOKEN]
            !school.isNullOrBlank() && !token.isNullOrBlank()
        }
        .distinctUntilChanged()

    suspend fun isRegisteredSnapshot(): Boolean = isRegistered.first()

    suspend fun credentialsOrNull(): RegistrationCredentials? {
        val prefs = dataStore.data.first()
        val schoolId = prefs[PreferencesKeys.STUDENT_SCHOOL_ID] ?: return null
        val syncToken = prefs[PreferencesKeys.STUDENT_SYNC_TOKEN] ?: return null
        val studentId = prefs[PreferencesKeys.STUDENT_ID] ?: return null
        if (schoolId.isBlank() || syncToken.isBlank() || studentId.isBlank()) return null
        return RegistrationCredentials(
            studentId = studentId,
            schoolId = schoolId,
            syncToken = syncToken,
        )
    }

    /**
     * Validates [schoolCode], calls `register_student`, and persists `school_id` + `sync_token`.
     */
    suspend fun registerWithSchoolCode(
        schoolCode: String,
        studentId: String,
        displayName: String,
    ) {
        val client = supabase
            ?: error("Supabase is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in local.properties.")
        val normalizedCode = SchoolCodeRules.validate(schoolCode)
        val validatedName = DisplayNameRules.validate(displayName)
        val deviceId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        ) ?: "unknown"

        val body = buildJsonObject {
            put("school_code", normalizedCode)
            put("student_id", studentId)
            put("display_name", validatedName)
            put("device_id", deviceId)
        }
        val result = client.rpc("register_student", body)
        val schoolId = result["school_id"]?.jsonPrimitive?.content
            ?: error("register_student: missing school_id")
        val syncToken = result["sync_token"]?.jsonPrimitive?.content
            ?: error("register_student: missing sync_token")

        dataStore.edit { prefs ->
            prefs[PreferencesKeys.STUDENT_SCHOOL_ID] = schoolId
            prefs[PreferencesKeys.STUDENT_SYNC_TOKEN] = syncToken
        }
    }
}

data class RegistrationCredentials(
    val studentId: String,
    val schoolId: String,
    val syncToken: String,
)
