package me.ash.reader.infrastructure.net

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.ash.reader.infrastructure.preference.AiPromptPreference
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** One-shot article summarization against any OpenAI compatible Chat Completions endpoint. */
@Singleton
class AiSummaryClient @Inject constructor(okHttpClient: OkHttpClient) {

    // Streaming responses idle longer than the shared client's 30s read timeout
    private val client =
        okHttpClient.newBuilder().readTimeout(120L, TimeUnit.SECONDS).build()

    fun streamSummary(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        article: String,
        thinkingEnabled: Boolean,
    ): Flow<AiChunk> =
        flow {
            val request =
                Request.Builder()
                    .url(baseUrl.trimEnd('/') + "/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(requestBody(model, prompt, article, thinkingEnabled).toRequestBody(JSON_TYPE))
                    .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${response.body.string().take(200)}")
                }
                val source = response.body.source()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    sseReasoning(line)?.let { emit(AiChunk.Reasoning(it)) }
                    sseDelta(line)?.let { emit(AiChunk.Content(it)) }
                }
            }
        }.flowOn(Dispatchers.IO)

    private fun requestBody(
        model: String,
        prompt: String,
        article: String,
        thinkingEnabled: Boolean,
    ): String =
        buildJsonObject {
                put("model", model)
                put("stream", true)
                // Chat Completions standard field, sent both ways: "high" guarantees models that
                // only think when asked actually think; "none" skips reasoning entirely.
                put("reasoning_effort", if (thinkingEnabled) "high" else "none")
                put(
                    "messages",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("role", "system")
                                put("content", prompt.ifBlank { SYSTEM_PROMPT })
                            }
                        )
                        add(
                            buildJsonObject {
                                put("role", "user")
                                put("content", article)
                            }
                        )
                    }
                )
            }
            .toString()

    companion object {

        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val SYSTEM_PROMPT = AiPromptPreference.default

        /** Extract reasoning_content (hidden thinking tokens) from one SSE line. */
        internal fun sseReasoning(line: String?): String? {
            if (line == null || !line.startsWith("data:")) return null
            val payload = line.substring(5).trim()
            if (payload.isEmpty() || payload == "[DONE]") return null
            return runCatching {
                    Json
                        .parseToJsonElement(payload)
                        .jsonObject["choices"]
                        ?.jsonArray
                        ?.firstOrNull()
                        ?.jsonObject
                        ?.get("delta")
                        ?.jsonObject
                        ?.get("reasoning_content")
                        ?.jsonPrimitive
                        ?.contentOrNull
                }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
        }

        /** Extract the streamed text from one SSE line; null for comments, blanks and [DONE]. */
        internal fun sseDelta(line: String?): String? {
            if (line == null || !line.startsWith("data:")) return null
            val payload = line.substring(5).trim()
            if (payload.isEmpty() || payload == "[DONE]") return null
            return runCatching {
                    Json
                        .parseToJsonElement(payload)
                        .jsonObject["choices"]
                        ?.jsonArray
                        ?.firstOrNull()
                        ?.jsonObject
                        ?.get("delta")
                        ?.jsonObject
                        ?.get("content")
                        ?.jsonPrimitive
                        ?.contentOrNull
                }
                .getOrNull()
                // Some endpoints pad every chunk with content:""; empty strings are not answer text
                ?.takeIf { it.isNotEmpty() }
        }
    }
}

/** One streamed piece from the model: reasoning tokens (hidden) or answer text (shown). */
sealed interface AiChunk {
    data class Reasoning(val text: String) : AiChunk
    data class Content(val text: String) : AiChunk
}
