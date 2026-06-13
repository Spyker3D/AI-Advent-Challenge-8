package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(branchId: String = "main"): List<Message> {
        return chatRepository.getMessages(branchId)
    }
}