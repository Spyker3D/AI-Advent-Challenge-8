package com.aiassistant.core.domain.memory

import javax.inject.Inject

class TaskStateMachine @Inject constructor() {

    fun nextStage(current: TaskStage): TaskStage? = when (current) {
        TaskStage.PLANNING -> TaskStage.EXECUTION
        TaskStage.EXECUTION -> TaskStage.VALIDATION
        TaskStage.VALIDATION -> TaskStage.DONE
        TaskStage.DONE -> null
    }

    fun canTransition(from: TaskStage, to: TaskStage): Boolean = when (from) {
        TaskStage.PLANNING -> to == TaskStage.EXECUTION
        TaskStage.EXECUTION -> to == TaskStage.VALIDATION
        TaskStage.VALIDATION -> to == TaskStage.DONE
        TaskStage.DONE -> false
    }

    fun moveToNextStage(taskContext: TaskContext): TaskContext {
        val currentStage = taskContext.taskState.stage
        val nextStage = nextStage(currentStage) ?: return taskContext
        if (!canTransition(currentStage, nextStage)) return taskContext

        val step = when (nextStage) {
            TaskStage.PLANNING -> "Planning agent is running"
            TaskStage.EXECUTION -> "Execution agent is running"
            TaskStage.VALIDATION -> "Validation agent is running"
            TaskStage.DONE -> "Done agent is running"
        }
        return taskContext.withState(nextStage, TaskRunStatus.RUNNING, step)
    }

    fun completeStage(taskContext: TaskContext, result: String): TaskContext {
        val updated = when (taskContext.taskState.stage) {
            TaskStage.PLANNING -> taskContext.copy(planningResult = result)
                .withStatus(TaskRunStatus.WAITING_USER, "Planning completed")

            TaskStage.EXECUTION -> taskContext.copy(executionResult = result)
                .withStatus(TaskRunStatus.WAITING_USER, "Execution completed")

            TaskStage.VALIDATION -> taskContext.copy(validationResult = result)
                .withStatus(TaskRunStatus.WAITING_USER, "Validation completed")

            TaskStage.DONE -> taskContext.copy(currentState = result)
                .withStatus(TaskRunStatus.COMPLETED, "Task completed")
        }
        return updated.copy(updatedAt = System.currentTimeMillis())
    }

    fun waitForUser(taskContext: TaskContext): TaskContext =
        taskContext.withStatus(TaskRunStatus.WAITING_USER, "Waiting for user confirmation")

    fun beginFeedback(taskContext: TaskContext): TaskContext {
        if (taskContext.taskState.status != TaskRunStatus.WAITING_USER) return taskContext
        return taskContext.withStatus(
            TaskRunStatus.RUNNING,
            "Updating ${taskContext.taskState.stage.name} result from user feedback"
        )
    }

    fun completeFeedback(taskContext: TaskContext, result: String): TaskContext {
        val updated = when (taskContext.taskState.stage) {
            TaskStage.PLANNING -> taskContext.copy(planningResult = result)
            TaskStage.EXECUTION -> taskContext.copy(executionResult = result)
            TaskStage.VALIDATION -> taskContext.copy(validationResult = result)
            TaskStage.DONE -> return taskContext
        }
        return updated.withStatus(
            TaskRunStatus.WAITING_USER,
            "${taskContext.taskState.stage.name.lowercase().replaceFirstChar(Char::uppercase)} updated"
        )
    }

    fun pause(taskContext: TaskContext): TaskContext {
        if (taskContext.taskState.status == TaskRunStatus.COMPLETED) return taskContext
        return taskContext.withStatus(TaskRunStatus.PAUSED, "Paused by user")
    }

    fun resume(taskContext: TaskContext): TaskContext {
        if (taskContext.taskState.status != TaskRunStatus.PAUSED) return taskContext
        return taskContext.withStatus(
            TaskRunStatus.RUNNING,
            "Resumed ${taskContext.taskState.stage.name}"
        )
    }

    private fun TaskContext.withState(
        stage: TaskStage,
        status: TaskRunStatus,
        step: String
    ): TaskContext = copy(
        taskState = taskState.copy(stage = stage, status = status, currentStep = step),
        updatedAt = System.currentTimeMillis()
    )

    private fun TaskContext.withStatus(
        status: TaskRunStatus,
        step: String
    ): TaskContext = copy(
        taskState = taskState.copy(status = status, currentStep = step),
        updatedAt = System.currentTimeMillis()
    )
}
