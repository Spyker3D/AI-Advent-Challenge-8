package com.aiassistant.core.domain.agent

import android.util.Log
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.ContextStrategy

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.StickyFacts
import com.aiassistant.core.domain.entity.TokenMetrics
import com.aiassistant.core.domain.util.TokenCounter
import com.aiassistant.core.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class ChatAgent @Inject constructor(
    private val chatRepository: ChatRepository,
    private val llmClient: LlmClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    // Current branch ID - default to "main"
    private var currentBranchId: String = "main"
    
    // Current sticky facts
    private var currentStickyFacts: StickyFacts = StickyFacts()
    
    // Set the current branch
    fun setCurrentBranch(branchId: String) {
        currentBranchId = branchId
    }
    
    // Set the current sticky facts
    fun setCurrentStickyFacts(stickyFacts: StickyFacts) {
        currentStickyFacts = stickyFacts
    }
    

    
    /**
     * Build system prompt with sticky facts
     */
    private fun buildSystemPromptWithFacts(originalPrompt: String?): String {
        return buildString {
            append(originalPrompt ?: "")
            append("\n\nImportant facts:\n\n")
            append("Goal:\n${currentStickyFacts.goal}\n\n")
            append("Stack:\n${currentStickyFacts.stack}\n\n")
            append("Constraints:\n${currentStickyFacts.constraints}\n\n")
            append("Preferences:\n${currentStickyFacts.preferences}\n\n")
            append("Decisions:\n${currentStickyFacts.decisions}\n\n")
            append("Unresolved Questions:\n${currentStickyFacts.unresolvedQuestions}\n\n")
        }
    }
    
    /**
     * Sends a message to the LLM and manages the chat history
     */
    suspend fun sendMessage(chatRequest: ChatRequest, contextStrategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW): Result<AiChatResponse> = withContext(dispatcher) {
        try {
            // Use the history provided in the chatRequest
            // The history already includes the user message from ViewModel, so we don't need to add it again
            val effectiveHistory = chatRequest.history.toMutableList()
            
            // Get the user message from the history (it should be the last message)
            val userMessage = effectiveHistory.lastOrNull { it.role == MessageRole.USER } ?: Message(
                id = UUID.randomUUID().toString(),
                content = chatRequest.message,
                role = MessageRole.USER
            )
            
            // Log the history size for debugging
            Log.d("ChatAgent", "Sending history size: ${effectiveHistory.size}")
            
            // Calculate token metrics before sending to LLM
            val currentRequestTokens = TokenCounter.countTokens(chatRequest.message)
            val historyTokens = effectiveHistory.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .sumOf { TokenCounter.countTokens(it.content) }
            
            // For sticky facts strategy, do not modify the system prompt as it's already handled by ViewModel
            // The system prompt with facts is already in the first message of effectiveHistory
            val effectiveSystemPrompt = chatRequest.systemPrompt
            
            // Send to LLM with full history and proper maxTokens
            // For STICKY_FACTS, the system message with facts is already in the history
            // For other strategies, we may need to update or add the system prompt
            val messagesToSend = if (effectiveHistory.isNotEmpty() && effectiveHistory.first().role == MessageRole.SYSTEM) {
                // If there's already a system message (e.g., from STICKY_FACTS), use it as-is
                // Otherwise, update the system prompt in the first message
                effectiveHistory
            } else {
                // Add system prompt as first message if not present
                val systemMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = effectiveSystemPrompt.toString(),
                    role = MessageRole.SYSTEM
                )
                listOf(systemMessage) + effectiveHistory
            }
            
            val result = llmClient.sendChat(messagesToSend, chatRequest.maxTokens, chatRequest.model.modelName)
            
            result.map { chatResponse ->
                // Create token metrics
                val tokenMetrics = TokenMetrics(
                    currentRequestTokens = currentRequestTokens,
                    historyTokens = historyTokens,
                    completionTokens = chatResponse.completionTokens
                )
                
                // Create assistant message with token metrics
                val assistantMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = chatResponse.message,
                    role = MessageRole.ASSISTANT,
                    tokenMetrics = tokenMetrics
                )
                
                // Save only the assistant message to repository with current branch
                // The user message is already saved by the ViewModel
                chatRepository.saveMessage(assistantMessage, currentBranchId)
                
                // Return the response with token metrics
                AiChatResponse(chatResponse.message, null, tokenMetrics)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sends a message with restrictions to the LLM and manages the chat history
     */
    suspend fun sendMessageWithRestrictions(
        chatRequest: ChatRequest,
        useJsonFormat: Boolean,
        limitLength: Boolean,
        useStopSequence: Boolean,
        stopSequenceText: String,
        contextStrategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW
    ): Result<AiChatResponse> = withContext(dispatcher) {
        try {
            // Use the history provided in the chatRequest
            // The history already includes the user message from ViewModel, so we don't need to add it again
            val effectiveHistory = chatRequest.history.toMutableList()
            
            // Get the user message from the history (it should be the last message)
            // If not found, create a new one with restrictions
            val userMessage = effectiveHistory.lastOrNull { it.role == MessageRole.USER }?.let { existingMessage ->
                // Apply restrictions to existing message if needed
                val restrictedContent = buildUserMessageWithRestrictions(
                    originalMessage = existingMessage.content,
                    useJsonFormat = useJsonFormat,
                    limitLength = limitLength
                )
                if (restrictedContent != existingMessage.content) {
                    existingMessage.copy(content = restrictedContent)
                } else {
                    existingMessage
                }
            } ?: Message(
                id = UUID.randomUUID().toString(),
                content = buildUserMessageWithRestrictions(
                    originalMessage = chatRequest.message,
                    useJsonFormat = useJsonFormat,
                    limitLength = limitLength
                ),
                role = MessageRole.USER
            )
            
            // Log the history size for debugging
            Log.d("ChatAgent", "Sending history size with restrictions: ${effectiveHistory.size}")
            
            // Calculate token metrics before sending to LLM
            val currentRequestTokens = TokenCounter.countTokens(chatRequest.message)
            val historyTokens = effectiveHistory.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .sumOf { TokenCounter.countTokens(it.content) }
            
            // For sticky facts strategy, do not modify the system prompt as it's already handled by ViewModel
            // The system prompt with facts is already in the first message of effectiveHistory
            val effectiveSystemPrompt = chatRequest.systemPrompt
            
            // Send to LLM with full history and proper maxTokens
            // For STICKY_FACTS, the system message with facts is already in the history
            // For other strategies, we may need to update or add the system prompt
            val messagesToSend = if (effectiveHistory.isNotEmpty() && effectiveHistory.first().role == MessageRole.SYSTEM) {
                // If there's already a system message (e.g., from STICKY_FACTS), use it as-is
                // Otherwise, update the system prompt in the first message
                effectiveHistory
            } else {
                // Add system prompt as first message if not present
                val systemMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = effectiveSystemPrompt.toString(),
                    role = MessageRole.SYSTEM
                )
                listOf(systemMessage) + effectiveHistory
            }
            
            val result = llmClient.sendChat(messagesToSend, chatRequest.maxTokens, chatRequest.model.modelName)
            
            result.map { chatResponse ->
                // Create token metrics
                val tokenMetrics = TokenMetrics(
                    currentRequestTokens = currentRequestTokens,
                    historyTokens = historyTokens,
                    completionTokens = chatResponse.completionTokens
                )
                
                // Create assistant message with token metrics
                val assistantMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = chatResponse.message,
                    role = MessageRole.ASSISTANT,
                    tokenMetrics = tokenMetrics
                )
                
                // Save only the assistant message to repository with current branch
                // The user message is already saved by the ViewModel
                chatRepository.saveMessage(assistantMessage, currentBranchId)
                
                // Return the response with token metrics
                AiChatResponse(chatResponse.message, null, tokenMetrics)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildUserMessageWithRestrictions(
        originalMessage: String,
        useJsonFormat: Boolean,
        limitLength: Boolean
    ): String {
        val stringBuilder = StringBuilder(originalMessage)
        
        if (useJsonFormat) {
            stringBuilder.append("\n\nReturn the answer strictly as valid JSON with this structure:\n{\n\"topic\": \"short topic\",\n\"date\": \"today's date in YYYY-MM-DD format\",\n\"time\": \"current time in HH:MM format\",\n\"answer\": \"main answer\",\n\"tags\": [\"tag1\", \"tag2\", \"tag3\"]\n}\nDo not add any text before or after JSON. For date and time fields, provide actual values, not placeholders.")
        }
        
        if (limitLength) {
            stringBuilder.append("\n\nKeep the answer short. Maximum 500 characters. Use no more than 3 sentences.")
        }
        
        return stringBuilder.toString()
    }
}