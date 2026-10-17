package com.premyer.aura.data

/**
 * A single chat message as shown in the UI.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val isError: Boolean = false,
    // Device data is never sent to a provider without a separate sharing confirmation.
    val localOnly: Boolean = false
)

enum class Role {
    USER, ASSISTANT
}

/** Thrown when the API key is missing so the UI can show a clear message. */
class MissingApiKeyException : Exception("Bitte zuerst einen KI-Anbieter in den Einstellungen konfigurieren.")

/** Thrown for any non-2xx response from the Anthropic API. */
class AnthropicApiException(val code: Int, message: String) : Exception(message)
