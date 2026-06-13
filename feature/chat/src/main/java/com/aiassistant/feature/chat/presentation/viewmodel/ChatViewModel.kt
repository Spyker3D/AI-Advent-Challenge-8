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
import com.aiassistant.core.domain.entity.ContextStrategy
import com.aiassistant.core.domain.entity.StickyFacts
import com.aiassistant.core.domain.entity.ChatBranch
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
        initializeBranches()
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
    
    private fun initializeBranches() {
        // Create default "main" branch with existing messages
        val mainBranch = ChatBranch(
            id = "main",
            name = "main",
            messages = _uiState.value.messages
        )
        
        _uiState.value = _uiState.value.copy(
            branches = listOf(mainBranch),
            currentBranchId = "main"
        )
    }
    
    private fun createBranch(branchName: String) {
        val newBranchId = UUID.randomUUID().toString()
        
        // Copy current branch messages to new branch
        val currentBranch = _uiState.value.branches.find { it.id == _uiState.value.currentBranchId }
        val newBranch = ChatBranch(
            id = newBranchId,
            name = branchName,
            messages = currentBranch?.messages ?: emptyList()
        )
        
        _uiState.value = _uiState.value.copy(
            branches = _uiState.value.branches + newBranch,
            currentBranchId = newBranchId
        )
    }
    
    private fun switchBranch(branchId: String) {
        // Update current branch ID
        _uiState.value = _uiState.value.copy(currentBranchId = branchId)
        
        // Update messages to match the current branch
        val currentBranch = _uiState.value.branches.find { it.id == branchId }
        if (currentBranch != null) {
            _uiState.value = _uiState.value.copy(messages = currentBranch.messages)
        }
    }
    
    private fun updateBranchWithMessage(branchId: String, message: Message): List<ChatBranch> {
        return _uiState.value.branches.map { branch ->
            if (branch.id == branchId) {
                branch.copy(messages = branch.messages + message)
            } else {
                branch
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
            is ChatUiEvent.ClearSummary -> {
                clearConversationSummary()
            }
            
            // Context strategy events
            is ChatUiEvent.ContextStrategySelected -> {
                _uiState.value = _uiState.value.copy(selectedContextStrategy = event.strategy)
            }
            is ChatUiEvent.CreateBranch -> {
                createBranch(event.branchName)
            }
            is ChatUiEvent.SwitchBranch -> {
                switchBranch(event.branchId)
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
        
        // For Sticky Facts strategy, update facts before sending message
        if (_uiState.value.selectedContextStrategy == ContextStrategy.STICKY_FACTS) {
            updateStickyFacts(finalMessage)
        }
        
        // Check if we need to generate a new summary BEFORE adding the user message
        // Generate summary only when: numberOfNewMessagesSinceLastSummary >= 10
        val shouldGenerateSummary = _uiState.value.useContextCompression && 
            (_uiState.value.messages.size + 1) >= _uiState.value.lastSummaryMessageCount + 10
            
        if (shouldGenerateSummary) {
            generateConversationSummary()
        }

        // Add user message to both the general messages list and the current branch
        val updatedMessages = _uiState.value.messages + userMessage
        
        // Update current branch with the new message
        val updatedBranches = updateBranchWithMessage(_uiState.value.currentBranchId, userMessage)
        
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            branches = updatedBranches,
            currentMessage = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                // Calculate token metrics before building the request
                val requestTokens = TokenCounter.countTokens(finalMessage)
                
                // Build the appropriate history based on the selected context strategy
                val (effectiveHistory, historyTokens) = when (_uiState.value.selectedContextStrategy) {
                    ContextStrategy.SLIDING_WINDOW -> {
                        buildSlidingWindowHistory(finalMessage, requestTokens)
                    }
                    ContextStrategy.STICKY_FACTS -> {
                        buildStickyFactsHistory(finalMessage, requestTokens)
                    }
                    ContextStrategy.BRANCHING -> {
                        buildBranchingHistory(finalMessage, requestTokens)
                    }
                }
                
                val chatRequest = ChatRequest(
                    message = finalMessage,
                    model = _uiState.value.selectedModel,
                    temperature = _uiState.value.temperature,
                    maxTokens = _uiState.value.maxTokens,
                    systemPrompt = _uiState.value.systemPrompt,
                    history = effectiveHistory
                )

                // Send message using ChatAgent with context strategy
                val result = if (_uiState.value.useJsonFormat || _uiState.value.limitLength || _uiState.value.useStopSequence) {
                    chatAgent.sendMessageWithRestrictions(
                        chatRequest = chatRequest,
                        useJsonFormat = _uiState.value.useJsonFormat,
                        limitLength = _uiState.value.limitLength,
                        useStopSequence = _uiState.value.useStopSequence,
                        stopSequenceText = _uiState.value.stopSequenceText,
                        contextStrategy = _uiState.value.selectedContextStrategy
                    )
                } else {
                    chatAgent.sendMessage(chatRequest, _uiState.value.selectedContextStrategy)
                }

                result
                    .onSuccess { response ->
                        // Create token metrics with the calculated values
                        val tokenMetrics = TokenMetrics(
                            currentRequestTokens = requestTokens,
                            historyTokens = historyTokens,
                            completionTokens = response.tokenMetrics?.completionTokens
                        )
                        
                        // Add assistant response to messages
                        val assistantMessage = Message(
                            id = UUID.randomUUID().toString(),
                            content = response.message,
                            role = MessageRole.ASSISTANT,
                            timestamp = System.currentTimeMillis(),
                            tokenMetrics = tokenMetrics
                        )
                        
                        // Add assistant message to both the general messages list and the current branch
                        val updatedMessages = _uiState.value.messages + assistantMessage
                        
                        // Update current branch with the new message
                        val updatedBranches = updateBranchWithMessage(_uiState.value.currentBranchId, assistantMessage)
                        
                        _uiState.value = _uiState.value.copy(
                            messages = updatedMessages,
                            branches = updatedBranches,
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
    
    private fun clearConversationSummary() {
        _uiState.value = _uiState.value.copy(
            conversationSummary = "",
            summaryMessageCount = 0,
            lastSummaryMessageCount = 0
        )
        // Update token estimates after clearing summary
        updateTokenEstimates()
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
                            summaryMessageCount = messagesToSummarize.size,
                            lastSummaryMessageCount = _uiState.value.messages.size
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
            // Note: We don't count the full history messages that were replaced by the summary
            val summaryTokens = TokenCounter.countTokens("Conversation Summary: ${_uiState.value.conversationSummary}")
            val lastMessagesTokens = messages.takeLast(_uiState.value.keepLastMessagesCount)
                .sumOf { TokenCounter.countTokens(it.content) }
            summaryTokens + lastMessagesTokens
        } else {
            // If compression is not enabled or no summary, use full history
            fullHistoryTokens
        }
        
        val savedTokens = fullHistoryTokens - compressedTokens
        
        // Calculate compression ratio with better precision handling
        val compressionRatio = if (fullHistoryTokens > 0 && compressedTokens < fullHistoryTokens) {
            ((fullHistoryTokens - compressedTokens).toLong() * 100 / fullHistoryTokens).toInt()
        } else {
            0
        }
        
        _uiState.value = _uiState.value.copy(
            fullHistoryTokensEstimate = fullHistoryTokens,
            compressedHistoryTokensEstimate = compressedTokens,
            savedTokensEstimate = savedTokens,
            compressionRatioPercent = compressionRatio
        )
    }
    
    // Context strategy helper methods
    
    private fun buildSlidingWindowHistory(finalMessage: String, requestTokens: Int): Pair<List<Message>, Int> {
        val SLIDING_WINDOW_SIZE = 5
        
        // Take the last SLIDING_WINDOW_SIZE messages
        val lastMessages = _uiState.value.messages.takeLast(SLIDING_WINDOW_SIZE)
        
        // Calculate history tokens
        val historyTokens = lastMessages.sumOf { TokenCounter.countTokens(it.content) }
        
        return Pair(lastMessages, historyTokens)
    }
    
    private fun buildStickyFactsHistory(finalMessage: String, requestTokens: Int): Pair<List<Message>, Int> {
        val SLIDING_WINDOW_SIZE = 5
        
        // Create system message with sticky facts
        val factsContent = buildString {
            append("Important facts:\n\n")
            append("Goal:\n${_uiState.value.stickyFacts.goal}\n\n")
            append("Stack:\n${_uiState.value.stickyFacts.stack}\n\n")
            append("Constraints:\n${_uiState.value.stickyFacts.constraints}\n\n")
            append("Preferences:\n${_uiState.value.stickyFacts.preferences}\n\n")
            append("Decisions:\n${_uiState.value.stickyFacts.decisions}\n\n")
            append("Unresolved Questions:\n${_uiState.value.stickyFacts.unresolvedQuestions}\n\n")
        }
        
        val factsMessage = Message(
            id = UUID.randomUUID().toString(),
            content = factsContent,
            role = MessageRole.SYSTEM
        )
        
        // Take the last SLIDING_WINDOW_SIZE messages
        val lastMessages = _uiState.value.messages.takeLast(SLIDING_WINDOW_SIZE)
        
        // Combine facts message with last messages
        val effectiveHistory = listOf(factsMessage) + lastMessages
        
        // Calculate history tokens (facts + last messages)
        val factsTokens = TokenCounter.countTokens(factsContent)
        val lastMessagesTokens = lastMessages.sumOf { TokenCounter.countTokens(it.content) }
        val historyTokens = factsTokens + lastMessagesTokens
        
        return Pair(effectiveHistory, historyTokens)
    }
    
    private fun buildBranchingHistory(finalMessage: String, requestTokens: Int): Pair<List<Message>, Int> {
        // Get messages from current branch only
        val currentBranch = _uiState.value.branches.find { it.id == _uiState.value.currentBranchId }
        val branchMessages = currentBranch?.messages ?: emptyList()
        
        // Calculate history tokens
        val historyTokens = branchMessages.sumOf { TokenCounter.countTokens(it.content) }
        
        return Pair(branchMessages, historyTokens)
    }
    
    private fun updateStickyFacts(userMessage: String) {
        viewModelScope.launch {
            try {
                // Update UI to show facts are updating
                _uiState.value = _uiState.value.copy(factsStatus = "Updating")
                
                // Create the prompt for fact extraction
                val factsPrompt = """You are a memory extraction system.

Update the stored facts using the latest user message.

Keep only important long-term information.

Store:
- goal
- stack
- constraints
- preferences
- decisions
- unresolved questions

Ignore:
- greetings
- small talk
- temporary messages

Return valid JSON only.

Existing facts:
${buildFactsJsonString()}

Latest user message:
$userMessage

Return:""".trimIndent()
                
                // Create a message for fact extraction
                val factsMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = factsPrompt,
                    role = MessageRole.USER
                )
                
                // Extract facts using GPT-4o Mini
                val result = llmClient.sendChat(
                    messages = listOf(factsMessage),
                    maxTokens = 500,
                    model = "openai/gpt-4o-mini" // Always use GPT-4o Mini for fact extraction
                )
                
                result
                    .onSuccess { response ->
                        try {
                            // Parse the JSON response
                            val factsJson = response.message.trim()
                            val facts = parseFactsJson(factsJson)
                            
                            // Update sticky facts in UI
                            _uiState.value = _uiState.value.copy(
                                stickyFacts = facts,
                                factsStatus = "Updated"
                            )
                        } catch (e: Exception) {
                            // Keep previous facts and show failure
                            _uiState.value = _uiState.value.copy(factsStatus = "Failed")
                            android.util.Log.e("ChatViewModel", "Failed to parse facts JSON", e)
                        }
                    }
                    .onFailure { throwable ->
                        // Keep previous facts and show failure
                        _uiState.value = _uiState.value.copy(factsStatus = "Failed")
                        android.util.Log.e("ChatViewModel", "Failed to extract facts: ${throwable.message}")
                    }
                
            } catch (e: Exception) {
                // Keep previous facts and show failure
                _uiState.value = _uiState.value.copy(factsStatus = "Failed")
                android.util.Log.e("ChatViewModel", "Failed to update sticky facts", e)
            }
        }
    }
    
    private fun buildFactsJsonString(): String {
        return """{
  "goal": "${_uiState.value.stickyFacts.goal}",
  "stack": "${_uiState.value.stickyFacts.stack}",
  "constraints": "${_uiState.value.stickyFacts.constraints}",
  "preferences": "${_uiState.value.stickyFacts.preferences}",
  "decisions": "${_uiState.value.stickyFacts.decisions}",
  "unresolvedQuestions": "${_uiState.value.stickyFacts.unresolvedQuestions}"
}""".trimIndent()
    }
    
    private fun parseFactsJson(jsonString: String): StickyFacts {
        // Simple JSON parsing for the specific format
        val cleanJson = jsonString
            .replace(Regex("^```json"), "")
            .replace(Regex("```$"), "")
            .trim()
        
        // Extract values using regex
        val goal = extractJsonValue(cleanJson, "goal")
        val stack = extractJsonValue(cleanJson, "stack")
        val constraints = extractJsonValue(cleanJson, "constraints")
        val preferences = extractJsonValue(cleanJson, "preferences")
        val decisions = extractJsonValue(cleanJson, "decisions")
        val unresolvedQuestions = extractJsonValue(cleanJson, "unresolvedQuestions")
        
        return StickyFacts(
            goal = goal,
            stack = stack,
            constraints = constraints,
            preferences = preferences,
            decisions = decisions,
            unresolvedQuestions = unresolvedQuestions
        )
    }
    
    private fun extractJsonValue(json: String, key: String): String {
        // Simple regex to extract value for a key
        val regex = "\"${key}\"\\s*:\\s*\"([^\"\\]*(\\.[^\"\\]*)*)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }
}