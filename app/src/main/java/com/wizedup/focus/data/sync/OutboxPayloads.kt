package com.wizedup.focus.data.sync

import kotlinx.serialization.Serializable

object PayloadKinds {
    const val SESSION_START = "session_start"
    const val SESSION_END = "session_end"
    const val BYPASS = "bypass"
}

@Serializable
data class SessionStartPayload(
    val student_id: String,
    val sync_token: String,
    val school_id: String,
    val client_session_id: String,
    val started_at: String,
)

@Serializable
data class SessionEndPayload(
    val student_id: String,
    val sync_token: String,
    val client_session_id: String,
    val ended_at: String,
    val duration_seconds: Int,
)

@Serializable
data class BypassPayload(
    val student_id: String,
    val sync_token: String,
    val client_session_id: String,
    val event_type: String,
    val event_at: String,
)
