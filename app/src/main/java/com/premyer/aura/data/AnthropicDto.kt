package com.premyer.aura.data

import com.google.gson.annotations.SerializedName

// ---- Request ----

data class AnthropicMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

// ---- Response ----

data class AnthropicContentBlock(
    val type: String,
    val text: String? = null
)

data class AnthropicResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<AnthropicContentBlock>? = null,
    @SerializedName("stop_reason") val stopReason: String? = null
) {
    /** Concatenates all text blocks into a single reply string. */
    fun combinedText(): String =
        content?.filter { it.type == "text" }?.mapNotNull { it.text }?.joinToString("\n") ?: ""
}

data class AnthropicErrorBody(
    val type: String? = null,
    val error: AnthropicErrorDetail? = null
)

data class AnthropicErrorDetail(
    val type: String? = null,
    val message: String? = null
)
