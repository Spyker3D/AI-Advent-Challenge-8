package com.aiassistant.core.domain.memory

import com.aiassistant.core.domain.entity.Message

data class MemoryContext(
    val shortTermMessages: List<Message>,
    val taskContext: TaskContext?,
    val longTermMemory: LongTermMemory
)
