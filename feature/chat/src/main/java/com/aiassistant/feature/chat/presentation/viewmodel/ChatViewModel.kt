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
import com.aiassistant.core.domain.entity.TokenMetrics
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.usecase.ClearChatHistoryUseCase
import com.aiassistant.core.domain.usecase.GetChatHistoryUseCase
import com.aiassistant.core.domain.usecase.GetChatSettingsUseCase
import com.aiassistant.core.domain.usecase.SaveChatSettingsUseCase
import com.aiassistant.core.domain.usecase.SendMessageUseCase
import com.aiassistant.core.domain.util.TokenCounter
import com.aiassistant.feature.chat.presentation.ChatUiEvent
import com.aiassistant.feature.chat.presentation.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatSettingsUseCase: GetChatSettingsUseCase,
    private val saveChatSettingsUseCase: SaveChatSettingsUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val chatAgent: ChatAgent,
    private val chatRepository: ChatRepository,
    private val llmClient: LlmClient
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
                    stopSequenceText = settings.stopSequenceText,
                    // Context compression fields
                    useContextCompression = settings.useContextCompression,
                    keepLastMessagesCount = settings.keepLastMessagesCount
                )
                // Update token estimates when settings change
                updateTokenEstimates()
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
            is ChatUiEvent.FileAttached -> {
                _uiState.value = _uiState.value.copy(
                    attachedFileName = event.fileName,
                    attachedFileText = event.fileContent
                )
            }
            is ChatUiEvent.ClearAttachedFile -> {
                _uiState.value = _uiState.value.copy(
                    attachedFileName = null,
                    attachedFileText = null
                )
            }
            is ChatUiEvent.UseContextCompressionChanged -> {
                updateContextCompressionSetting(event.useContextCompression)
            }
            is ChatUiEvent.KeepLastMessagesCountChanged -> {
                updateKeepLastMessagesCount(event.count)
            }
            is ChatUiEvent.ClearSummary -> {
                clearConversationSummary()
            }
        }
    }

    private fun sendMessage() {
        val currentMessage = _uiState.value.currentMessage.trim()
        if (currentMessage.isBlank() || _uiState.value.isLoading) return

        // Combine message with attached file content if present
        val finalMessage = if (_uiState.value.attachedFileText != null) {
            "User question:\n$currentMessage\n\nAttached file content:\n${_uiState.value.attachedFileText}"
        } else {
            currentMessage
        }
        
        // Debug logging
        if (_uiState.value.attachedFileText != null) {
            android.util.Log.d("ChatViewModel", "File attached: true")
            android.util.Log.d("ChatViewModel", "File name: ${_uiState.value.attachedFileName}")
            android.util.Log.d("ChatViewModel", "Attached file characters: ${_uiState.value.attachedFileText?.length ?: 0}")
            android.util.Log.d("ChatViewModel", "Final prompt characters: ${finalMessage.length}")
        } else {
            android.util.Log.d("ChatViewModel", "File attached: false")
        }

        // Add user message immediately to UI for responsiveness
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = finalMessage,
            role = MessageRole.USER,
            timestamp = System.currentTimeMillis()
        )
        
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            currentMessage = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                // Check if we need to generate a new summary
                if (_uiState.value.useContextCompression && 
                    _uiState.value.messages.size >= _uiState.value.summaryMessageCount + 10) {
                    generateConversationSummary()
                }
                
                // Build the appropriate history based on context compression settings
                val effectiveHistory = if (_uiState.value.useContextCompression && 
                    _uiState.value.conversationSummary.isNotEmpty()) {
                    // Create compressed history with summary and last N messages
                    val summaryMessage = Message(
                        id = UUID.randomUUID().toString(),
                        content = "Conversation Summary: ${_uiState.value.conversationSummary}",
                        role = MessageRole.SYSTEM
                    )
                    
                    // Take the last N messages
                    val lastMessages = _uiState.value.messages.takeLast(_uiState.value.keepLastMessagesCount)
                    
                    // Combine summary and last messages
                    listOf(summaryMessage) + lastMessages
                } else {
                    // Use full history
                    _uiState.value.messages
                }
                
                val chatRequest = ChatRequest(
                    message = finalMessage,
                    model = _uiState.value.selectedModel,
                    temperature = _uiState.value.temperature,
                    maxTokens = _uiState.value.maxTokens,
                    systemPrompt = _uiState.value.systemPrompt,
                    history = effectiveHistory
                )

                // Send message using ChatAgent for both cases
                val result = if (_uiState.value.useJsonFormat || _uiState.value.limitLength || _uiState.value.useStopSequence) {
                    chatAgent.sendMessageWithRestrictions(
                        chatRequest = chatRequest,
                        useJsonFormat = _uiState.value.useJsonFormat,
                        limitLength = _uiState.value.limitLength,
                        useStopSequence = _uiState.value.useStopSequence,
                        stopSequenceText = _uiState.value.stopSequenceText
                    )
                } else {
                    chatAgent.sendMessage(chatRequest)
                }

                result
                    .onSuccess { response ->
                        // Add assistant response to messages
                        val assistantMessage = Message(
                            id = UUID.randomUUID().toString(),
                            content = response.message,
                            role = MessageRole.ASSISTANT,
                            timestamp = System.currentTimeMillis(),
                            tokenMetrics = response.tokenMetrics
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + assistantMessage,
                            isLoading = false,
                            // Clear attached file after sending
                            attachedFileName = null,
                            attachedFileText = null
                        )
                        
                        // Update token estimates after sending message
                        updateTokenEstimates()
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
    
    // Context compression helper methods
    
    private fun updateContextCompressionSetting(useContextCompression: Boolean) {
        viewModelScope.launch {
            try {
                val currentSettings = getChatSettingsUseCase().first()
                val updatedSettings = currentSettings.copy(useContextCompression = useContextCompression)
                saveChatSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun updateKeepLastMessagesCount(count: Int) {
        viewModelScope.launch {
            try {
                val currentSettings = getChatSettingsUseCase().first()
                val updatedSettings = currentSettings.copy(keepLastMessagesCount = count)
                saveChatSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun clearConversationSummary() {
        _uiState.value = _uiState.value.copy(
            conversationSummary = "",
            summaryMessageCount = 0
        )
    }
    
    private fun generateConversationSummary() {
        viewModelScope.launch {
            try {
                // Get messages to summarize (excluding the last keepLastMessagesCount messages)
                val messagesToSummarize = _uiState.value.messages.dropLast(_uiState.value.keepLastMessagesCount)
                
                if (messagesToSummarize.isEmpty()) return@launch
                
                // Create a conversation string for summarization
                val conversationText = messagesToSummarize.joinToString("\n\n") { message ->
                    "${message.role.value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}: ${message.content}"
                }
                
                // Limit the conversation text to prevent exceeding token limits
                val limitedConversationText = if (conversationText.length > 10000) {
                    conversationText.take(10000) + "\n\n[Conversation truncated due to length]"
                } else {
                    conversationText
                }
                
                // Create the prompt for summarization
                val summaryPrompt = """Summarize the following conversation.

Keep:
- important user goals
- important facts
- decisions
- preferences
- unresolved questions
- current task

Remove:
- greetings
- repetitions
- small talk

Return concise structured text.

Conversation:
$limitedConversationText""".trimIndent()
                
                // Create a message for summarization
                val summaryMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = summaryPrompt,
                    role = MessageRole.USER
                )
                
                // Generate summary using OpenAI GPT-4o Mini
                val result = llmClient.sendChat(
                    messages = listOf(summaryMessage),
                    maxTokens = 500, // Limit summary to 500 tokens
                    model = "openai/gpt-4o-mini" // Always use GPT-4o Mini for summaries
                )
                
                result
                    .onSuccess { response ->
                        // Update the UI state with the new summary
                        _uiState.value = _uiState.value.copy(
                            conversationSummary = response.message,
                            summaryMessageCount = messagesToSummarize.size
                        )
                        
                        // Update token estimates
                        updateTokenEstimates()
                        
                        // Log summary generation info
                        android.util.Log.d("ChatViewModel", "Generated summary for ${messagesToSummarize.size} messages. Summary length: ${response.message.length}")
                    }
                    .onFailure { throwable ->
                        // Handle error silently or show user-friendly error
                        android.util.Log.e("ChatViewModel", "Failed to generate conversation summary: ${throwable.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update conversation summary."
                        )
                    }
                
            } catch (e: Exception) {
                // Handle error silently or show user-friendly error
                android.util.Log.e("ChatViewModel", "Failed to generate conversation summary", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update conversation summary."
                )
            }
        }
    }
    
    private fun updateTokenEstimates() {
        val messages = _uiState.value.messages
        val fullHistoryTokens = messages.sumOf { TokenCounter.countTokens(it.content) }
        
        val compressedTokens = if (_uiState.value.useContextCompression && _uiState.value.conversationSummary.isNotEmpty()) {
            // Estimate tokens for summary + last N messages
            val summaryTokens = TokenCounter.countTokens("Conversation Summary: ${_uiState.value.conversationSummary}")
            val lastMessagesTokens = messages.takeLast(_uiState.value.keepLastMessagesCount)
                .sumOf { TokenCounter.countTokens(it.content) }
            summaryTokens + lastMessagesTokens
        } else {
            // If compression is not enabled or no summary, use full history
            fullHistoryTokens
        }
        
        val savedTokens = fullHistoryTokens - compressedTokens
        
        _uiState.value = _uiState.value.copy(
            fullHistoryTokensEstimate = fullHistoryTokens,
            compressedHistoryTokensEstimate = compressedTokens,
            savedTokensEstimate = savedTokens
        )
    }
}