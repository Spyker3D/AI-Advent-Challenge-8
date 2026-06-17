package com.aiassistant.core.domain.memory

data class TaskContext(
    val id: String,
    val title: String,
    val description: String,
    val relatedChatIds: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
    val decisions: List<String> = emptyList(),
    val currentState: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
