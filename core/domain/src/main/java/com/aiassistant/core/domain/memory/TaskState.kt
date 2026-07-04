package com.aiassistant.core.domain.memory

enum class TaskStage {
    PLANNING,
    EXECUTION,
    VALIDATION,
    DONE
}

enum class TaskRunStatus {
    RUNNING,
    WAITING_USER,
    PAUSED,
    COMPLETED
}

data class TaskState(
    val stage: TaskStage = TaskStage.PLANNING,
    val status: TaskRunStatus = TaskRunStatus.RUNNING,
    val currentStep: String = ""
)
