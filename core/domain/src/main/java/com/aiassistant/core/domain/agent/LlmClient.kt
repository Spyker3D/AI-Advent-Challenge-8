package com.aiassistant.core.domain.agent

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.AiResponseMetadata

interface LlmClient {
    suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String? = null): Result<ChatResponse>
}

data class ChatResponse(
    val message: String,
    val completionTokens: Int? = null,
    val metadata: AiResponseMetadata? = null
)
