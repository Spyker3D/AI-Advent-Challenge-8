package com.aiassistant.core.domain.rag

import com.aiassistant.core.domain.memory.TaskContext
import javax.inject.Inject

class TaskMemoryPromptFormatter @Inject constructor() {
    fun format(taskContext: TaskContext?): String {
        if (taskContext == null) return "None"

        return buildString {
            appendLine("Title:")
            appendLine(taskContext.title.ifBlank { "N/A" })
            appendLine()
            appendLine("Description:")
            appendLine(taskContext.description.ifBlank { "N/A" })
            appendLine()
            appendList("Goals", taskContext.goals)
            appendList("Constraints", taskContext.constraints)
            appendList("Decisions", taskContext.decisions)
            appendLine("Current State:")
            appendLine(taskContext.currentState.ifBlank { "N/A" })
        }.trim()
    }

    private fun StringBuilder.appendList(title: String, values: List<String>) {
        appendLine("$title:")
        if (values.isEmpty()) {
            appendLine("- N/A")
        } else {
            values.forEach { value -> appendLine("- $value") }
        }
        appendLine()
    }
}
