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
    val taskState: TaskState = TaskState(),
    val planningResult: String = "",
    val executionResult: String = "",
    val validationResult: String = "",
    val planningSwarmResults: List<PlanningSwarmResult> = emptyList(),
    val blockedByInvariantsMessage: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
