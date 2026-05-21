package com.wizedup.focus.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wizedup.focus.WizedUpApplication
import com.wizedup.focus.data.sync.PayloadKinds
import com.wizedup.focus.util.FocusDiag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class OutboxSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        val app = applicationContext as WizedUpApplication
        val client = app.supabaseRestClient ?: return Result.success()
        val outbox = app.outboxRepository

        while (true) {
            val batch = outbox.oldestBatch()
            if (batch.isEmpty()) return Result.success()
            val row = batch.first()
            try {
                val body = json.parseToJsonElement(row.payloadJson).jsonObject
                when (row.payloadKind) {
                    PayloadKinds.SESSION_START ->
                        client.rpc("submit_focus_session_start", body)
                    PayloadKinds.SESSION_END ->
                        client.rpc("submit_focus_session_end", body)
                    PayloadKinds.BYPASS ->
                        client.rpc("submit_bypass_event", body)
                    else -> {
                        FocusDiag.w("OutboxSyncWorker: unknown kind ${row.payloadKind}, dropping row")
                        outbox.deleteById(row.id)
                    }
                }
                if (row.payloadKind in setOf(
                        PayloadKinds.SESSION_START,
                        PayloadKinds.SESSION_END,
                        PayloadKinds.BYPASS,
                    )
                ) {
                    outbox.deleteById(row.id)
                }
            } catch (e: Exception) {
                FocusDiag.w("OutboxSyncWorker retry: ${e.message}")
                return Result.retry()
            }
        }
    }
}
