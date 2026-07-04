package com.aiassistant.core.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskMemoryMergerTest {
    private val merger = TaskMemoryMerger()

    @Test
    fun `merge adds stable memory without duplicates`() {
        val task = taskContext(
            goals = listOf("Implement Day25"),
            constraints = listOf("Use existing Working Memory"),
            decisions = listOf("Do not create separate memory")
        )

        val updated = merger.merge(
            taskContext = task,
            update = TaskContextUpdate(
                goalsAdd = listOf("Implement Day25", "Add recent history"),
                constraintsAdd = listOf("Use existing Working Memory"),
                decisionsAdd = listOf("Run RAG every turn")
            )
        )

        assertEquals(listOf("Implement Day25", "Add recent history"), updated.goals)
        assertEquals(listOf("Use existing Working Memory"), updated.constraints)
        assertEquals(listOf("Do not create separate memory", "Run RAG every turn"), updated.decisions)
    }

    @Test
    fun `merge stores clarifications and terms in current state without duplicate sections`() {
        val task = taskContext(
            currentState = """
                |Implementation in progress.
                |
                |Clarifications:
                |- RAG runs every turn
                |
                |Terms:
                |- TaskContext
            """.trimMargin()
        )

        val updated = merger.merge(
            taskContext = task,
            update = TaskContextUpdate(
                clarificationsAdd = listOf("RAG runs every turn", "Sources and quotes are required"),
                termsAdd = listOf("TaskContext", "Recent history")
            )
        )

        assertTrue(updated.currentState.contains("Implementation in progress."))
        assertTrue(updated.currentState.contains("- Sources and quotes are required"))
        assertTrue(updated.currentState.contains("- Recent history"))
        assertEquals(1, Regex("Clarifications:").findAll(updated.currentState).count())
        assertEquals(1, Regex("Terms:").findAll(updated.currentState).count())
    }

    private fun taskContext(
        goals: List<String> = emptyList(),
        constraints: List<String> = emptyList(),
        decisions: List<String> = emptyList(),
        currentState: String = ""
    ): TaskContext {
        return TaskContext(
            id = "day25",
            title = "Day25",
            description = "Mini-chat with RAG memory",
            goals = goals,
            constraints = constraints,
            decisions = decisions,
            currentState = currentState
        )
    }
}
