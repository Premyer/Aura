package com.premyer.aura.data

import kotlinx.coroutines.CancellationException

/**
 * Keeps the conversation history and mediates between the ViewModel and
 * the Anthropic API client.
 */
class AssistantRepository(
    private val apiService: AuraApiService = AuraApiService()
) {
    suspend fun sendMessage(history: List<ChatMessage>, config: ProviderConfig): Result<String> {
        return try {
            Result.success(apiService.sendMessage(history, config))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
