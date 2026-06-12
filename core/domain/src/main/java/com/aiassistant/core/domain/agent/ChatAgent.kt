package com.aiassistant.core.domain.agent

import android.util.Log
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
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
    
    /**
     * Sends a message to the LLM and manages the chat history
     */
    suspend fun sendMessage(chatRequest: ChatRequest): Result<AiChatResponse> = withContext(dispatcher) {
        try {
            // Get current chat history from repository
            val history = chatRepository.getMessages().toMutableList()
            
            // Check if we should apply context compression
            val isUsingCompressedHistory = chatRequest.history.isNotEmpty() && 
                chatRequest.history.firstOrNull()?.content?.startsWith("Conversation Summary:") == true
            
            val effectiveHistory = if (isUsingCompressedHistory) {
                // Use the compressed history provided in the request
                chatRequest.history.toMutableList()
            } else {
                // Use the full history from the repository
                history
            }
            
            // Create user message
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = chatRequest.message,
                role = MessageRole.USER
            )
            
            // For compressed history, we don't add the user message as it's already included
            if (!isUsingCompressedHistory) {
                // Add user message to history only if we're not using compressed history
                effectiveHistory.add(userMessage)
            }
            
            // Log the history size for debugging
            Log.d("ChatAgent", "Sending history size: ${effectiveHistory.size}")
            
            // Calculate token metrics before sending to LLM
            val currentRequestTokens = TokenCounter.countTokens(chatRequest.message)
            val historyTokens = effectiveHistory.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .sumOf { TokenCounter.countTokens(it.content) }
            
            // Send to LLM with full history and proper maxTokens
            val result = llmClient.sendChat(effectiveHistory, chatRequest.maxTokens, chatRequest.model.modelName)
            
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
                
                // Save both messages to repository
                chatRepository.saveMessage(userMessage)
                chatRepository.saveMessage(assistantMessage)
                
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
        stopSequenceText: String
    ): Result<AiChatResponse> = withContext(dispatcher) {
        try {
            // Get current chat history from repository
            val history = chatRepository.getMessages().toMutableList()
            
            // Check if we should apply context compression
            val isUsingCompressedHistory = chatRequest.history.isNotEmpty() && 
                chatRequest.history.firstOrNull()?.content?.startsWith("Conversation Summary:") == true
            
            val effectiveHistory = if (isUsingCompressedHistory) {
                // Use the compressed history provided in the request
                chatRequest.history.toMutableList()
            } else {
                // Use the full history from the repository
                history
            }
            
            // Create user message with restrictions
            val userMessageContent = buildUserMessageWithRestrictions(
                originalMessage = chatRequest.message,
                useJsonFormat = useJsonFormat,
                limitLength = limitLength
            )
            
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = userMessageContent,
                role = MessageRole.USER
            )
            
            // For compressed history, we don't add the user message as it's already included
            if (!isUsingCompressedHistory) {
                // Add user message to history only if we're not using compressed history
                effectiveHistory.add(userMessage)
            }
            
            // Log the history size for debugging
            Log.d("ChatAgent", "Sending history size with restrictions: ${effectiveHistory.size}")
            
            // Calculate token metrics before sending to LLM
            val currentRequestTokens = TokenCounter.countTokens(chatRequest.message)
            val historyTokens = effectiveHistory.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .sumOf { TokenCounter.countTokens(it.content) }
            
            // Send to LLM with full history and proper maxTokens
            val result = llmClient.sendChat(effectiveHistory, chatRequest.maxTokens, chatRequest.model.modelName)
            
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
                
                // Save both messages to repository
                chatRepository.saveMessage(userMessage)
                chatRepository.saveMessage(assistantMessage)
                
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