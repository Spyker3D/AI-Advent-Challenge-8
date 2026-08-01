package com.aiassistant.feature.chat.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiStateSendTest {

    @Test
    fun `active request rejects duplicate send`() {
        val state = ChatUiState(currentMessage = "Second message", isLoading = true)

        assertFalse(state.canSendMessage())
    }

    @Test
    fun `send becomes available after successful completion`() {
        val active = ChatUiState(currentMessage = "Next message", isLoading = true)
        val completed = active.copy(isLoading = false, error = null)

        assertTrue(completed.canSendMessage())
    }

    @Test
    fun `send becomes available after failed completion`() {
        val active = ChatUiState(currentMessage = "Retry message", isLoading = true)
        val failed = active.copy(isLoading = false, error = "Request failed")

        assertTrue(failed.canSendMessage())
    }

    @Test
    fun `blank message remains rejected when idle`() {
        assertFalse(ChatUiState(currentMessage = "   ", isLoading = false).canSendMessage())
    }
}