package com.aiassistant.core.domain.memory

sealed interface TaskWaitingUserResult {
    data class ContextUpdated(
        val taskContext: TaskContext,
        val resultWasEdited: Boolean = false
    ) : TaskWaitingUserResult

    data class QuestionAnswered(
        val answer: String,
        val taskContext: TaskContext
    ) : TaskWaitingUserResult

    data class Blocked(
        val message: String,
        val taskContext: TaskContext
    ) : TaskWaitingUserResult
}
