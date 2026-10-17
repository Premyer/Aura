package com.premyer.aura.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class ApiProtocol(val label: String) {
    OPENAI("OpenAI Chat Completions"), ANTHROPIC("Anthropic Messages"), GEMINI("Google Gemini")
}

/** Base URLs include the API version, but not /chat/completions or /messages. */
enum class Provider(val label: String, val baseUrl: String, val protocol: ApiProtocol) {
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", ApiProtocol.ANTHROPIC),
    OPENAI("OpenAI", "https://api.openai.com/v1", ApiProtocol.OPENAI),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", ApiProtocol.GEMINI),
    MISTRAL("Mistral", "https://api.mistral.ai/v1", ApiProtocol.OPENAI),
    GROQ("Groq", "https://api.groq.com/openai/v1", ApiProtocol.OPENAI),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", ApiProtocol.OPENAI),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", ApiProtocol.OPENAI),
    XAI("xAI", "https://api.x.ai/v1", ApiProtocol.OPENAI),
    CUSTOM("Eigener Anbieter", "", ApiProtocol.OPENAI)
}

data class ProviderConfig(
    val provider: Provider = Provider.ANTHROPIC,
    val protocol: ApiProtocol = provider.protocol,
    val baseUrl: String = provider.baseUrl,
    val model: String = "",
    val apiKey: String = ""
) {
    fun validate(): String? {
        val url = baseUrl.toHttpUrlOrNull() ?: return "Bitte eine gültige API-Basis-URL eingeben."
        if (!url.isHttps) return "API-Endpunkte müssen HTTPS verwenden."
        if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null) {
            return "Die Basis-URL darf keine Zugangsdaten, Query-Parameter oder Fragmente enthalten."
        }
        if (model.isBlank()) return "Bitte eine Modell-ID vom Anbieter eintragen."
        if (apiKey.isBlank()) return "Bitte einen API-Schlüssel eintragen."
        return null
    }
}
