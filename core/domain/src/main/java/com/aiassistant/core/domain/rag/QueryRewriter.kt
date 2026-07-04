package com.aiassistant.core.domain.rag

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.memory.TaskContext

interface QueryRewriter {
    suspend fun rewrite(question: String): String

    suspend fun rewrite(
        question: String,
        taskContext: TaskContext?,
        recentMessages: List<Message>
    ): String = rewrite(question)
}
