package com.premyer.aura.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.premyer.aura.device.DeviceAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AuraApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
) {
    private val gson = Gson()

    suspend fun sendMessage(history: List<ChatMessage>, config: ProviderConfig): String =
        withContext(Dispatchers.IO) {
            config.validate()?.let { throw IllegalArgumentException(it) }
            val wire = buildRequest(history, config)
            client.newCall(wire).execute().use { response ->
                if (!response.isSuccessful) {
                    // Do not echo response bodies: they may contain credentials or private data.
                    throw IllegalStateException(
                        "${config.provider.label}: HTTP ${response.code}. API-Schlüssel, Modell, Kontingent und Basis-URL prüfen."
                    )
                }
                val json = JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
                parseReply(json, config.protocol).ifBlank {
                    throw IllegalStateException("Keine Textantwort erhalten. Prüfe, ob das Modell Text-Chat unterstützt.")
                }
            }
        }

    internal fun buildRequest(history: List<ChatMessage>, config: ProviderConfig): Request {
        val messages = history.filterNot { it.isError || it.localOnly }
        val prompt = "Du bist Aura, ein hilfreicher Android-Assistent. Antworte auf Deutsch, sofern nicht anders gewünscht. " +
            DeviceAction.systemInstructions
        val base = config.baseUrl.trimEnd('/').toHttpUrl()
        val builder = Request.Builder()
        val payload: Map<String, Any>
        when (config.protocol) {
            ApiProtocol.OPENAI -> {
                builder.url(base.newBuilder().addPathSegments("chat/completions").build())
                    .header("Authorization", "Bearer ${config.apiKey}")
                payload = mapOf(
                    "model" to config.model,
                    "messages" to listOf(mapOf("role" to "system", "content" to prompt)) +
                        messages.map { mapOf("role" to if (it.role == Role.USER) "user" else "assistant", "content" to it.text) }
                )
            }
            ApiProtocol.ANTHROPIC -> {
                builder.url(base.newBuilder().addPathSegment("messages").build())
                    .header("x-api-key", config.apiKey).header("anthropic-version", "2023-06-01")
                payload = mapOf(
                    "model" to config.model, "max_tokens" to 2048, "system" to prompt,
                    "messages" to messages.map {
                        mapOf("role" to if (it.role == Role.USER) "user" else "assistant", "content" to it.text)
                    }
                )
            }
            ApiProtocol.GEMINI -> {
                builder.url(base.newBuilder().addPathSegment("models")
                    .addPathSegment("${config.model}:generateContent").build())
                    .header("x-goog-api-key", config.apiKey)
                payload = mapOf(
                    "systemInstruction" to mapOf("parts" to listOf(mapOf("text" to prompt))),
                    "contents" to messages.map {
                        mapOf("role" to if (it.role == Role.USER) "user" else "model",
                            "parts" to listOf(mapOf("text" to it.text)))
                    }
                )
            }
        }
        return builder.post(gson.toJson(payload).toRequestBody("application/json".toMediaType())).build()
    }

    internal fun parseReply(json: JsonObject, protocol: ApiProtocol): String = when (protocol) {
        ApiProtocol.OPENAI -> json.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("message")?.get("content")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
        ApiProtocol.ANTHROPIC -> json.getAsJsonArray("content")?.mapNotNull {
            it.asJsonObject.takeIf { block -> block.get("type")?.asString == "text" }?.get("text")?.asString
        }?.joinToString("\n").orEmpty()
        ApiProtocol.GEMINI -> json.getAsJsonArray("candidates")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("content")?.getAsJsonArray("parts")?.mapNotNull {
                it.asJsonObject.get("text")?.asString
            }?.joinToString("\n").orEmpty()
    }
}
