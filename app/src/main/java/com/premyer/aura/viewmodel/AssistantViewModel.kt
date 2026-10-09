package com.premyer.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.premyer.aura.data.AssistantRepository
import com.premyer.aura.data.ChatMessage
import com.premyer.aura.data.MissingApiKeyException
import com.premyer.aura.data.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

class AssistantViewModel(
    private val repository: AssistantRepository = AssistantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isSending) return

        val userMessage = ChatMessage(role = Role.USER, text = text)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isSending = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val historySnapshot = _uiState.value.messages
            val result = repository.sendMessage(historySnapshot)

            result.onSuccess { replyText ->
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(role = Role.ASSISTANT, text = replyText),
                        isSending = false
                    )
                }
            }.onFailure { throwable ->
                val friendlyMessage = when (throwable) {
                    is MissingApiKeyException ->
                        "Kein Anthropic API-Key konfiguriert. Trage ANTHROPIC_API_KEY in local.properties ein."
                    else -> throwable.message ?: "Unbekannter Fehler"
                }
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = friendlyMessage,
                        messages = it.messages + ChatMessage(
                            role = Role.ASSISTANT,
                            text = friendlyMessage,
                            isError = true
                        )
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
