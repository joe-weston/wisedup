package com.wizedup.focus.data.sync

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OutboxRepository(
    private val appContext: Context,
    private val dao: PendingSyncDao,
) {
    private val json = Json { encodeDefaults = true }

    suspend fun enqueueSessionStart(payload: SessionStartPayload) {
        dao.insert(
            PendingSyncEntity(
                payloadKind = PayloadKinds.SESSION_START,
                payloadJson = json.encodeToString(payload),
                createdAtMs = System.currentTimeMillis(),
            ),
        )
        SyncWorkScheduler.schedule(appContext)
    }

    suspend fun enqueueSessionEnd(payload: SessionEndPayload) {
        dao.insert(
            PendingSyncEntity(
                payloadKind = PayloadKinds.SESSION_END,
                payloadJson = json.encodeToString(payload),
                createdAtMs = System.currentTimeMillis(),
            ),
        )
        SyncWorkScheduler.schedule(appContext)
    }

    suspend fun enqueueBypass(payload: BypassPayload) {
        dao.insert(
            PendingSyncEntity(
                payloadKind = PayloadKinds.BYPASS,
                payloadJson = json.encodeToString(payload),
                createdAtMs = System.currentTimeMillis(),
            ),
        )
        SyncWorkScheduler.schedule(appContext)
    }

    suspend fun oldestBatch(): List<PendingSyncEntity> = dao.oldestBatch()

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
