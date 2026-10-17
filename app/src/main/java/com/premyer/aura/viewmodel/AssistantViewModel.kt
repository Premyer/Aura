package com.premyer.aura.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.premyer.aura.data.Provider
import com.premyer.aura.data.ProviderConfig
import com.premyer.aura.data.ProviderStore
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
    val errorMessage: String? = null,
    val config: ProviderConfig = ProviderConfig(),
    val showSettings: Boolean = false
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AssistantRepository()
    private val store = ProviderStore(application)

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        val config = runCatching { store.load(store.selected()) }.getOrElse { ProviderConfig() }
        _uiState.update { it.copy(config = config, showSettings = config.validate() != null) }
    }

    fun openSettings() = _uiState.update { it.copy(showSettings = true) }
    fun closeSettings() = _uiState.update { it.copy(showSettings = false) }

    fun loadProvider(provider: Provider): ProviderConfig =
        runCatching { store.load(provider) }.getOrElse { ProviderConfig(provider = provider) }

    /** Returns an error message, or null on success. */
    fun saveConfig(config: ProviderConfig): String? {
        config.validate()?.let { return it }
        return runCatching { store.save(config) }.fold(
            onSuccess = { _uiState.update { it.copy(config = config, showSettings = false) }; null },
            onFailure = { it.message ?: "Speichern fehlgeschlagen." }
        )
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isSending) return
        if (_uiState.value.config.validate() != null) { openSettings(); return }

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
            val result = repository.sendMessage(historySnapshot, _uiState.value.config)

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
                        "Kein API-Schlüssel eingetragen. Öffne die Einstellungen oben rechts."
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
