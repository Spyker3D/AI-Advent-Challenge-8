package com.aiassistant.core.domain.agent

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.AiResponseMetadata

interface LlmClient {
    suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String? = null): Result<ChatResponse>

    suspend fun sendChat(
        messages: List<Message>,
        maxTokens: Int?,
        model: String?,
        options: LlmRequestOptions
    ): Result<ChatResponse> = sendChat(messages, maxTokens, model)
}

data class LlmRequestOptions(
    val temperature: Double? = null,
    val numPredict: Int? = null,
    val stream: Boolean? = null,
    val jsonSchema: String? = null
)

data class ChatResponse(
    val message: String,
    val completionTokens: Int? = null,
    val metadata: AiResponseMetadata? = null
)
