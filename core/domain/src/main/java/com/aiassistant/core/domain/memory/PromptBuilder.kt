package com.aiassistant.core.domain.memory

import com.aiassistant.core.domain.invariant.Invariant
import javax.inject.Inject

class PromptBuilder @Inject constructor() {
    fun buildSystemPrompt(
        baseSystemPrompt: String,
        memoryContext: MemoryContext,
        invariants: List<Invariant>
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
            appendInvariantSection(invariants)
        }
    }

    fun buildSystemPrompt(
        baseSystemPrompt: String,
        invariants: List<Invariant>
    ): String = buildString {
        appendLine(baseSystemPrompt)
        appendInvariantSection(invariants)
    }

    fun buildInvariantSection(invariants: List<Invariant>): String = buildString {
        appendInvariantSection(invariants)
    }

    private fun StringBuilder.appendInvariantSection(invariants: List<Invariant>) {
        appendLine()
        appendLine("=========================")
        appendLine("INVARIANTS")
        appendLine("=========================")
        appendLine()
        invariants.forEach { appendLine("- ${it.description}") }
        appendLine()
        appendLine("Нарушение любого инварианта запрещено.")
        appendLine("Если пользователь просит решение, нарушающее инварианты, откажись и предложи допустимый вариант.")
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

}
