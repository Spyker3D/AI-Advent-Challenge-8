package com.aiassistant.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.usecase.GetChatSettingsUseCase
import com.aiassistant.core.domain.usecase.SendMessageUseCase
import com.aiassistant.feature.chat.presentation.ChatUiEvent
import com.aiassistant.feature.chat.presentation.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatSettingsUseCase: GetChatSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeChatSettings()
    }

    private fun observeChatSettings() {
        getChatSettingsUseCase()
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    selectedModel = settings.selectedModel,
                    temperature = settings.temperature,
                    maxTokens = settings.maxTokens,
                    systemPrompt = settings.systemPrompt
                )
            }
            .launchIn(viewModelScope)
    }

    fun handleEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.MessageChanged -> {
                _uiState.value = _uiState.value.copy(currentMessage = event.message)
            }
            is ChatUiEvent.SendMessage -> {
                sendMessage()
            }
            is ChatUiEvent.ClearError -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
            is ChatUiEvent.ClearChat -> {
                _uiState.value = _uiState.value.copy(messages = emptyList())
            }
        }
    }

    private fun sendMessage() {
        val currentMessage = _uiState.value.currentMessage.trim()
        if (currentMessage.isBlank() || _uiState.value.isLoading) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = currentMessage,
            role = MessageRole.USER
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            currentMessage = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val chatRequest = ChatRequest(
                message = currentMessage,
                model = _uiState.value.selectedModel,
                temperature = _uiState.value.temperature,
                maxTokens = _uiState.value.maxTokens,
                systemPrompt = _uiState.value.systemPrompt
            )

            sendMessageUseCase(chatRequest)
                .onSuccess { response ->
                    val assistantMessage = Message(
                        id = UUID.randomUUID().toString(),
                        content = response,
                        role = MessageRole.ASSISTANT
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isLoading = false
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "An unknown error occurred"
                    )
                }
        }
    }
}