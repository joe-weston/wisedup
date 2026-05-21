package com.wizedup.focus.compliance

import com.wizedup.focus.data.FocusState
import com.wizedup.focus.data.FocusStateRepository
import com.wizedup.focus.data.SchoolRegistrationRepository
import com.wizedup.focus.data.sync.OutboxRepository
import com.wizedup.focus.data.sync.SessionEndPayload
import com.wizedup.focus.data.sync.SessionStartPayload
import com.wizedup.focus.util.FocusDiag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Observes focus-state edges and enqueues R2 compliance payloads (ADR-005).
 */
class ComplianceCoordinator(
    private val scope: CoroutineScope,
    private val focusRepo: FocusStateRepository,
    private val schoolRepo: SchoolRegistrationRepository,
    private val outbox: OutboxRepository,
    private val backendConfigured: () -> Boolean,
) {
    fun start() {
        scope.launch {
            var initialized = false
            var prev = FocusState.Inactive
            focusRepo.state.collect { next ->
                if (!initialized) {
                    initialized = true
                    prev = next
                    return@collect
                }

                if (backendConfigured() && schoolRepo.isRegisteredSnapshot()) {
                    val creds = schoolRepo.credentialsOrNull()
                    if (creds != null) {
                        if (!prev.isActive && next.isActive) {
                            val sid = next.clientSessionId
                            val startedMs = next.startedAtMs
                            if (sid != null && startedMs != null) {
                                val payload = SessionStartPayload(
                                    student_id = creds.studentId,
                                    sync_token = creds.syncToken,
                                    school_id = creds.schoolId,
                                    client_session_id = sid,
                                    started_at = Instant.ofEpochMilli(startedMs).toString(),
                                )
                                runCatching { outbox.enqueueSessionStart(payload) }
                                    .onFailure { FocusDiag.w("ComplianceCoordinator start enqueue failed: $it") }
                            }
                        }
                        if (prev.isActive && !next.isActive) {
                            val sid = prev.clientSessionId
                            val startedMs = prev.startedAtMs
                            if (sid != null && startedMs != null) {
                                val endedMs = System.currentTimeMillis()
                                val duration = ((endedMs - startedMs) / 1000L).toInt().coerceAtLeast(0)
                                val payload = SessionEndPayload(
                                    student_id = creds.studentId,
                                    sync_token = creds.syncToken,
                                    client_session_id = sid,
                                    ended_at = Instant.ofEpochMilli(endedMs).toString(),
                                    duration_seconds = duration,
                                )
                                runCatching { outbox.enqueueSessionEnd(payload) }
                                    .onFailure { FocusDiag.w("ComplianceCoordinator end enqueue failed: $it") }
                            }
                        }
                    }
                }

                prev = next
            }
        }
    }
}
