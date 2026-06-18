package com.aiassistant.core.domain.memory

import javax.inject.Inject

class PromptBuilder @Inject constructor() {
    fun buildSystemPrompt(
        baseSystemPrompt: String,
        memoryContext: MemoryContext
    ): String {
        val taskContext = memoryContext.taskContext

        return buildString {
            appendLine(baseSystemPrompt)
            appendLine()
            appendLine("=========================")
            appendLine("USER_PROFILE")
            appendLine("=========================")
            appendLine()
            appendLine(memoryContext.longTermMemory.profile)
            appendLine()
            appendLine("=========================")
            appendLine("USER_PREFERENCES")
            appendLine("=========================")
            appendLine()
            appendLine(memoryContext.longTermMemory.preferences)
            appendLine()
            appendLine("=========================")
            appendLine("LONG_TERM_MEMORY")
            appendLine("=========================")
            appendLine()
            appendLine("Global Rules:")
            appendLine(memoryContext.longTermMemory.globalRules)
            appendLine()
            appendLine("Project Knowledge:")
            appendLine(memoryContext.longTermMemory.projectKnowledge)
            appendLine()
            appendLine("Decisions:")
            appendLine(memoryContext.longTermMemory.decisions)
            appendLine()
            appendLine("=========================")
            appendLine("WORKING_MEMORY")
            appendLine("=========================")
            appendLine()
            appendLine("Task Id:")
            appendLine(taskContext?.id.orEmpty())
            appendLine()
            appendLine("Title:")
            appendLine(taskContext?.title.orEmpty())
            appendLine()
            appendLine("Description:")
            appendLine(taskContext?.description.orEmpty())
            appendLine()
            appendLine("Goals:")
            appendLine(taskContext?.goals?.joinToString("\n").orEmpty())
            appendLine()
            appendLine("Constraints:")
            appendLine(taskContext?.constraints?.joinToString("\n").orEmpty())
            appendLine()
            appendLine("Decisions:")
            appendLine(taskContext?.decisions?.joinToString("\n").orEmpty())
            appendLine()
            appendLine("Current State:")
            appendLine(taskContext?.currentState.orEmpty())
            appendLine()
            appendTaskState(taskContext)
            appendLine("=========================")
            appendLine("SHORT_TERM_MEMORY")
            appendLine("=========================")
            appendLine()
            appendLine("Short-term memory is passed separately")
            appendLine("as recent chat messages.")
        }
    }

    fun buildTaskStagePrompt(taskContext: TaskContext): String = buildString {
        appendTaskState(taskContext)
        appendLine()
        appendLine("=========================")
        appendLine("STAGE_INSTRUCTIONS")
        appendLine("=========================")
        appendLine()
        appendLine(
            when (taskContext.taskState.stage) {
                TaskStage.PLANNING -> PLANNING_PROMPT
                TaskStage.EXECUTION -> EXECUTION_PROMPT
                    .replace("{planningResult}", taskContext.planningResult)
                TaskStage.VALIDATION -> VALIDATION_PROMPT
                    .replace("{executionResult}", taskContext.executionResult)
                TaskStage.DONE -> "The task is complete. Do not produce another stage result."
            }
        )
    }

    private fun StringBuilder.appendTaskState(taskContext: TaskContext?) {
        appendLine("=========================")
        appendLine("TASK_STATE")
        appendLine("=========================")
        appendLine()
        appendLine("Stage:")
        appendLine(taskContext?.taskState?.stage?.name.orEmpty())
        appendLine()
        appendLine("Status:")
        appendLine(taskContext?.taskState?.status?.name.orEmpty())
        appendLine()
        appendLine("Current step:")
        appendLine(taskContext?.taskState?.currentStep.orEmpty())
        appendLine()
        appendLine("Planning result:")
        appendLine(taskContext?.planningResult.orEmpty())
        appendLine()
        appendLine("Execution result:")
        appendLine(taskContext?.executionResult.orEmpty())
        appendLine()
        appendLine("Validation result:")
        appendLine(taskContext?.validationResult.orEmpty())
    }

    private companion object {
        const val PLANNING_PROMPT = """You are a planning agent.

Your task:
- analyze the user request
- define goals
- define constraints
- create a step-by-step plan
- do not create the final result yet

Return only the planning result."""

        const val EXECUTION_PROMPT = """You are an execution agent.

Your task:
- use the approved planning result
- follow TaskContext goals and constraints
- produce the final result requested by the user

Approved planning result:
{planningResult}

Return only the execution result."""

        const val VALIDATION_PROMPT = """You are a validation agent.

Your task:
- validate the execution result
- check if it matches goals and constraints
- find issues and contradictions
- suggest fixes if needed

Execution result:
{executionResult}

Return only the validation result."""
    }
}
