package com.aiassistant.core.domain.memory

sealed interface TaskTransitionResult {
    data class Allowed(
        val taskContext: TaskContext
    ) : TaskTransitionResult

    data class Blocked(
        val message: String,
        val taskContext: TaskContext
    ) : TaskTransitionResult
}
