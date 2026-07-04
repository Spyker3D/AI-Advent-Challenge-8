package com.aiassistant.core.domain.rag

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentHistoryFormatterTest {
    private val formatter = RecentHistoryFormatter()

    @Test
    fun `format uses recent messages and role labels`() {
        val messages = listOf(
            message("old", MessageRole.USER),
            message("How to add Task Memory?", MessageRole.USER),
            message("Use existing Working Memory.", MessageRole.ASSISTANT)
        )

        val formatted = formatter.format(messages, maxMessages = 2, maxChars = 1000)

        assertEquals(false, formatted.contains("old"))
        assertTrue(formatted.contains("User: How to add Task Memory?"))
        assertTrue(formatted.contains("Assistant: Use existing Working Memory."))
    }

    @Test
    fun `format limits long output`() {
        val formatted = formatter.format(
            messages = listOf(message("x".repeat(1000), MessageRole.USER)),
            maxMessages = 1,
            maxChars = 80,
            messagePreviewChars = 200
        )

        assertTrue(formatted.length <= 83)
    }

    private fun message(content: String, role: MessageRole): Message {
        return Message(
            id = content.take(8),
            content = content,
            role = role
        )
    }
}
