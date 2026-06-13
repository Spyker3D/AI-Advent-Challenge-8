package com.aiassistant.core.domain.repository

import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.FormattedAiResponse
import com.aiassistant.core.domain.entity.Message

interface ChatRepository {
    suspend fun sendMessage(chatRequest: ChatRequest): Result<AiChatResponse>
    
    /**
     * Deprecated: Use ChatAgent.sendMessageWithRestrictions instead
     */
    suspend fun sendMessageWithRestrictions(
        chatRequest: ChatRequest,
        useJsonFormat: Boolean,
        limitLength: Boolean,
        useStopSequence: Boolean,
        stopSequenceText: String
    ): Result<AiChatResponse>
    
    fun parseFormattedResponse(response: String): Result<FormattedAiResponse>
    
    // Methods for chat history management
    suspend fun saveMessage(message: Message, branchId: String = "main"): Unit
    suspend fun getMessages(branchId: String = "main"): List<Message>
    suspend fun clearMessages(branchId: String = "main"): Unit
}
