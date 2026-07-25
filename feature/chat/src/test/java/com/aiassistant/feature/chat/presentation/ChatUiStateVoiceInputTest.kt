package com.aiassistant.feature.chat.presentation

import com.aiassistant.feature.chat.voice.VoiceInputState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiStateVoiceInputTest {
    @Test
    fun `preparing voice input preserves nonempty draft and requests permission`() {
        val result = ChatUiState(currentMessage = "buy bread").prepareForVoiceInput()

        assertEquals("buy bread", result.currentMessage)
        assertTrue(result.voiceInputState is VoiceInputState.PermissionRequired)
    }

    @Test
    fun `preparing voice input preserves unrelated state`() {
        val initial = ChatUiState(
            currentMessage = "buy bread",
            isLoading = true,
            error = "existing error",
            isChatDrawerOpen = true
        )

        val result = initial.prepareForVoiceInput()

        assertEquals(
            initial.copy(
                voiceInputState = VoiceInputState.PermissionRequired
            ),
            result
        )
    }
}
