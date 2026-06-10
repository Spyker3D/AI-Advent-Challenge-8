package com.aiassistant.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.agent.ChatAgent
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.FormattedAiResponse
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.usecase.ClearChatHistoryUseCase
import com.aiassistant.core.domain.usecase.GetChatHistoryUseCase
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
    private val getChatSettingsUseCase: GetChatSettingsUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val chatAgent: ChatAgent,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeChatSettings()
        loadChatHistory()
    }

    private fun observeChatSettings() {
        getChatSettingsUseCase()
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    selectedModel = settings.selectedModel,
                    temperature = settings.temperature,
                    maxTokens = settings.maxTokens,
                    systemPrompt = settings.systemPrompt,
                    // Day 2 fields
                    useJsonFormat = settings.useJsonFormat,
                    limitLength = settings.limitLength,
                    useStopSequence = settings.useStopSequence,
                    stopSequenceText = settings.stopSequenceText
                )
            }
            .launchIn(viewModelScope)
    }
    
    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                val messages = getChatHistoryUseCase()
                _uiState.value = _uiState.value.copy(messages = messages)
            } catch (e: Exception) {
                // Handle error silently or log it
            }
        }
    }
    
    fun refreshSettings() {
        observeChatSettings()
    }
    
    private fun clearChatHistory() {
        viewModelScope.launch {
            try {
                clearChatHistoryUseCase()
            } catch (e: Exception) {
                // Handle error silently or log it
            }
        }
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
                clearChatHistory()
            }
            is ChatUiEvent.ModelSelected -> {
                _uiState.value = _uiState.value.copy(selectedModel = event.model)
            }
        }
    }

    private fun sendMessage() {
        val currentMessage = _uiState.value.currentMessage.trim()
        if (currentMessage.isBlank() || _uiState.value.isLoading) return

        // Note: With the new ChatAgent, the user message will be handled internally
        // We still update the UI state immediately for responsiveness
        _uiState.value = _uiState.value.copy(
            currentMessage = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val chatRequest = ChatRequest(
                    message = currentMessage,
                    model = _uiState.value.selectedModel,
                    temperature = _uiState.value.temperature,
                    maxTokens = if (_uiState.value.limitLength) 250 else _uiState.value.maxTokens,
                    systemPrompt = _uiState.value.systemPrompt
                )

                // Send message with restrictions if any are enabled
                val result = if (_uiState.value.useJsonFormat || _uiState.value.limitLength || _uiState.value.useStopSequence) {
                    chatAgent.sendMessageWithRestrictions(
                        chatRequest = chatRequest,
                        useJsonFormat = _uiState.value.useJsonFormat,
                        limitLength = _uiState.value.limitLength,
                        useStopSequence = _uiState.value.useStopSequence,
                        stopSequenceText = _uiState.value.stopSequenceText
                    )
                } else {
                    sendMessageUseCase(chatRequest)
                }

                result
                    .onSuccess { response ->
                        // Reload messages from repository since ChatAgent handles persistence
                        val updatedMessages = chatRepository.getMessages()
                        _uiState.value = _uiState.value.copy(
                            messages = updatedMessages,
                            isLoading = false
                        )
                    }
                    .onFailure { throwable ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "An unknown error occurred"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    // Helper function to parse FormattedAiResponse
    fun parseFormattedResponse(response: String): FormattedAiResponse? {
        return try {
            chatRepository.parseFormattedResponse(response).getOrNull()
        } catch (e: Exception) {
            null
        }
    }


}