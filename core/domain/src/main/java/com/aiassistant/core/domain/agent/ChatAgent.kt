package com.aiassistant.core.domain.agent

import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
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
            
            // Create user message
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = chatRequest.message,
                role = MessageRole.USER
            )
            
            // Add user message to history
            history.add(userMessage)
            
            // Send to LLM with full history
            val result = llmClient.sendChat(history)
            
            result.map { assistantMessageContent ->
                // Create assistant message
                val assistantMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = assistantMessageContent,
                    role = MessageRole.ASSISTANT
                )
                
                // Save both messages to repository
                chatRepository.saveMessage(userMessage)
                chatRepository.saveMessage(assistantMessage)
                
                // Return the response with null metadata (will be populated by the client)
                AiChatResponse(assistantMessageContent, null)
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
        // For this refactoring, we'll delegate to the existing repository method
        // A more complete implementation would handle these restrictions in the agent
        chatRepository.sendMessageWithRestrictions(
            chatRequest,
            useJsonFormat,
            limitLength,
            useStopSequence,
            stopSequenceText
        )
    }
}