package com.premyer.aura.data

/**
 * Keeps the conversation history and mediates between the ViewModel and
 * the Anthropic API client.
 */
class AssistantRepository(
    private val apiService: AuraApiService = AuraApiService()
) {
    suspend fun sendMessage(history: List<ChatMessage>): Result<String> {
        return try {
            Result.success(apiService.sendMessage(history))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
