package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String = "main"): List<Message> {
        return chatRepository.getMessages(chatId)
    }
}