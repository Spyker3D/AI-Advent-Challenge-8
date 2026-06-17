package com.aiassistant.core.domain.memory

import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.repository.LongTermMemoryRepository
import com.aiassistant.core.domain.repository.WorkingMemoryRepository
import javax.inject.Inject

class MemoryOrchestrator @Inject constructor(
    private val chatRepository: ChatRepository,
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository
) {
    suspend fun buildMemoryContext(
        chatId: String,
        taskContextId: String?,
        shortTermLimit: Int = 5
    ): MemoryContext {
        val shortTermMessages = chatRepository
            .getMessages(chatId)
            .takeLast(shortTermLimit)

        val taskContext = taskContextId
            ?.let { workingMemoryRepository.getTaskContext(it) }
            ?: workingMemoryRepository.getActiveTaskContext()

        val longTermMemory = longTermMemoryRepository.getLongTermMemory()

        return MemoryContext(
            shortTermMessages = shortTermMessages,
            taskContext = taskContext,
            longTermMemory = longTermMemory
        )
    }
}
