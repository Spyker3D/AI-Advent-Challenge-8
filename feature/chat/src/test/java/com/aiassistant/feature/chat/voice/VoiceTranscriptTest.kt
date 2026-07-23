package com.aiassistant.feature.chat.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTranscriptTest {
    @Test
    fun `transcript replaces an empty draft`() {
        assertEquals("Hello world", mergeVoiceTranscript("", " Hello world "))
    }

    @Test
    fun `transcript is appended to an existing draft`() {
        assertEquals("Existing text dictated text", mergeVoiceTranscript("Existing text  ", "dictated text"))
    }

    @Test
    fun `blank transcript leaves draft unchanged`() {
        assertEquals("Existing text", mergeVoiceTranscript("Existing text", "  "))
    }
}
