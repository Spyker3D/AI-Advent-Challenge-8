package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.repository.ChatRepository
import javax.inject.Inject

class ClearChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String = "main") {
        chatRepository.clearMessages(chatId)
    }
}