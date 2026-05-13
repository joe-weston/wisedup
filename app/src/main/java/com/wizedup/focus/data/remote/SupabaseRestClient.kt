package com.wizedup.focus.data.remote

import com.wizedup.focus.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class SupabaseRpcException(
    val statusCode: Int,
    message: String,
) : Exception("HTTP $statusCode: $message")

/**
 * Minimal PostgREST RPC client (no supabase-kt) — calls `/rest/v1/rpc/<fn>` per ADR-005.
 */
class SupabaseRestClient(
    baseUrl: String,
    /** Supabase publishable key (PostgREST `apikey` + `Authorization: Bearer`). */
    private val publishableKey: String,
) {
    private val root = baseUrl.trimEnd('/')

    private val parser = Json { ignoreUnknownKeys = true }

    private val http = HttpClient(Android) {
        install(ContentNegotiation) {
            json(parser)
        }
    }

    suspend fun rpc(function: String, body: JsonObject): JsonObject {
        val url = "$root/rest/v1/rpc/$function"
        val response = http.post(url) {
            header("apikey", publishableKey)
            header("Authorization", "Bearer $publishableKey")
            contentType(ContentType.Application.Json)
            setBody(parser.encodeToString(body))
        }
        val text = response.bodyAsText()
        if (response.status.value >= 300) {
            throw SupabaseRpcException(response.status.value, text)
        }
        return parser.parseToJsonElement(text).jsonObject
    }

    companion object {
        fun fromBuildConfig(): SupabaseRestClient? {
            val url = BuildConfig.SUPABASE_URL.trim()
            val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
            if (url.isEmpty() || key.isEmpty()) return null
            return SupabaseRestClient(url, key)
        }
    }
}
