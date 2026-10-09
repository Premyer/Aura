package com.premyer.aura.data

import com.google.gson.Gson
import com.premyer.aura.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Talks to Anthropic's Messages API directly from the app.
 *
 * NOTE ON SECURITY: the API key is embedded in the compiled app via
 * BuildConfig (read from local.properties, which is gitignored and never
 * committed). This is convenient for development, but anyone can extract
 * an API key from a published APK. Before shipping this app publicly
 * (e.g. to the Play Store), move these calls behind your own backend
 * (e.g. the Cloud Run service this project used before) so the key never
 * leaves a server you control.
 */
class AuraApiService(
    private val apiKey: String = BuildConfig.ANTHROPIC_API_KEY,
    private val model: String = BuildConfig.ANTHROPIC_MODEL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val ANTHROPIC_MESSAGES_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val SYSTEM_PROMPT =
            "Du bist Aura, ein hilfreicher, freundlicher KI-Assistent in einer Android-App. " +
                "Antworte klar und auf den Punkt."
    }

    /**
     * Sends the full conversation history (oldest first) to Claude and
     * returns the assistant's reply text.
     */
    suspend fun sendMessage(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw MissingApiKeyException()
        }

        val anthropicMessages = history.map {
            AnthropicMessage(
                role = if (it.role == Role.USER) "user" else "assistant",
                content = it.text
            )
        }

        val requestPayload = AnthropicRequest(
            model = model,
            maxTokens = 1024,
            system = SYSTEM_PROMPT,
            messages = anthropicMessages
        )

        val jsonBody = gson.toJson(requestPayload)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(ANTHROPIC_MESSAGES_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string() ?: "{}"

            if (!response.isSuccessful) {
                val message = runCatching {
                    gson.fromJson(responseBody, AnthropicErrorBody::class.java)
                        .error?.message
                }.getOrNull() ?: "HTTP ${response.code}"
                throw AnthropicApiException(response.code, message)
            }

            val parsed = gson.fromJson(responseBody, AnthropicResponse::class.java)
            parsed.combinedText().ifBlank { "(Keine Antwort erhalten)" }
        }
    }
}
