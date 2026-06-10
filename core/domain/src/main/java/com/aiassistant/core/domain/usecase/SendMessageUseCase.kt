package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.agent.ChatAgent
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.ChatRequest
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatAgent: ChatAgent
) {
    suspend operator fun invoke(chatRequest: ChatRequest): Result<AiChatResponse> {
        return chatAgent.sendMessage(chatRequest)
    }
}
