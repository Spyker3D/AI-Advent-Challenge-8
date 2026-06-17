package com.aiassistant.feature.chat.presentation.memory

import com.aiassistant.core.domain.memory.LongTermMemory
import com.aiassistant.core.domain.memory.TaskContext

data class MemoryUiState(
    val activeTaskContext: TaskContext? = null,
    val longTermMemory: LongTermMemory = LongTermMemory(
        profile = "",
        preferences = "",
        globalRules = "",
        projectKnowledge = "",
        decisions = ""
    ),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
