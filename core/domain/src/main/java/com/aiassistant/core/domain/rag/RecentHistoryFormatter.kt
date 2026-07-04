package com.aiassistant.core.domain.rag

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import javax.inject.Inject

class RecentHistoryFormatter @Inject constructor() {
    fun format(
        messages: List<Message>,
        maxMessages: Int = DEFAULT_MAX_MESSAGES,
        maxChars: Int = DEFAULT_MAX_CHARS,
        messagePreviewChars: Int = DEFAULT_MESSAGE_PREVIEW_CHARS
    ): String {
        if (messages.isEmpty()) return "None"

        val selected = messages.takeLast(maxMessages)
        val formatted = selected.joinToString(separator = "\n") { message ->
            "${message.role.label()}: ${message.content.preview(messagePreviewChars)}"
        }
        return formatted.preview(maxChars)
    }

    private fun MessageRole.label(): String {
        return when (this) {
            MessageRole.USER -> "User"
            MessageRole.ASSISTANT -> "Assistant"
            MessageRole.SYSTEM -> "System"
        }
    }

    private fun String.preview(maxChars: Int): String {
        val compact = replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxChars) compact else compact.take(maxChars) + "..."
    }

    companion object {
        private const val DEFAULT_MAX_MESSAGES = 8
        private const val DEFAULT_MAX_CHARS = 3000
        private const val DEFAULT_MESSAGE_PREVIEW_CHARS = 500
    }
}
