package com.wizedup.focus.compliance

import com.wizedup.focus.data.FocusStateRepository
import com.wizedup.focus.data.SchoolRegistrationRepository
import com.wizedup.focus.data.sync.BypassPayload
import com.wizedup.focus.data.sync.OutboxRepository
import java.time.Instant

object ComplianceBypassReporter {
    suspend fun reportAccessibilityDisabled(
        focusRepo: FocusStateRepository,
        schoolRepo: SchoolRegistrationRepository,
        outbox: OutboxRepository,
        backendConfigured: Boolean,
    ) {
        if (!backendConfigured) return
        if (!schoolRepo.isRegisteredSnapshot()) return
        val creds = schoolRepo.credentialsOrNull() ?: return
        val snap = focusRepo.snapshot()
        if (!snap.isActive) return
        val sid = snap.clientSessionId ?: return
        val payload = BypassPayload(
            student_id = creds.studentId,
            sync_token = creds.syncToken,
            client_session_id = sid,
            event_type = "accessibility_disabled",
            event_at = Instant.now().toString(),
        )
        outbox.enqueueBypass(payload)
    }
}
