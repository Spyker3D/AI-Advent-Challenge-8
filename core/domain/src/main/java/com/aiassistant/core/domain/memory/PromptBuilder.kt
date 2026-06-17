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
            appendLine("LONG_TERM_MEMORY")
            appendLine("=========================")
            appendLine()
            appendLine("Profile:")
            appendLine(memoryContext.longTermMemory.profile)
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
            appendLine("=========================")
            appendLine("SHORT_TERM_MEMORY")
            appendLine("=========================")
            appendLine()
            appendLine("Short-term memory is passed separately")
            appendLine("as recent chat messages.")
        }
    }
}
