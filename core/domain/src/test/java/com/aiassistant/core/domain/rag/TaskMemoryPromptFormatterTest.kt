package com.aiassistant.core.domain.rag

import com.aiassistant.core.domain.memory.TaskContext
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskMemoryPromptFormatterTest {
    private val formatter = TaskMemoryPromptFormatter()

    @Test
    fun `null task memory formats as none`() {
        assertEquals("None", formatter.format(null))
    }

    @Test
    fun `blank fields and empty lists use explicit fallbacks`() {
        val formatted = formatter.format(
            taskContext(
                title = " ",
                description = "",
                currentState = "\t"
            )
        )

        assertEquals(
            """
            Title:
            N/A

            Description:
            N/A

            Goals:
            - N/A

            Constraints:
            - N/A

            Decisions:
            - N/A

            Current State:
            N/A
            """.trimIndent(),
            formatted
        )
    }

    @Test
    fun `populated task memory preserves values and list order`() {
        val formatted = formatter.format(
            taskContext(
                title = "Ship search",
                description = "Add local retrieval",
                goals = listOf("Index files", "Return matches"),
                constraints = listOf("No network"),
                decisions = listOf("Use cosine similarity", "Cache embeddings"),
                currentState = "Validating"
            )
        )

        assertEquals(
            """
            Title:
            Ship search

            Description:
            Add local retrieval

            Goals:
            - Index files
            - Return matches

            Constraints:
            - No network

            Decisions:
            - Use cosine similarity
            - Cache embeddings

            Current State:
            Validating
            """.trimIndent(),
            formatted
        )
    }

    private fun taskContext(
        title: String,
        description: String,
        goals: List<String> = emptyList(),
        constraints: List<String> = emptyList(),
        decisions: List<String> = emptyList(),
        currentState: String
    ) = TaskContext(
        id = "task-1",
        title = title,
        description = description,
        goals = goals,
        constraints = constraints,
        decisions = decisions,
        currentState = currentState
    )
}
