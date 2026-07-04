package com.aiassistant.core.domain.memory

import com.aiassistant.core.domain.entity.Message

interface TaskMemoryUpdater {
    suspend fun updateFromConversation(
        taskContext: TaskContext,
        recentMessages: List<Message>
    ): TaskContextUpdate
}
