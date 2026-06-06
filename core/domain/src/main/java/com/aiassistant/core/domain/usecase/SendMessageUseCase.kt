package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatRequest: ChatRequest): Result<String> {
        return chatRepository.sendMessage(chatRequest)
    }
}