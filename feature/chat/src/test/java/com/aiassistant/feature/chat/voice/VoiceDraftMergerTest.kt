package com.aiassistant.feature.chat.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceDraftMergerTest {
    @Test
    fun `recognized segment appends to existing draft with one separator`() {
        assertEquals(
            "Typed text recognized words",
            VoiceDraftMerger.merge("Typed text  ", "  recognized   words ")
        )
    }

    @Test
    fun `recognized segment becomes draft when existing text is empty`() {
        assertEquals("recognized words", VoiceDraftMerger.merge("", " recognized   words "))
    }

    @Test
    fun `blank recognition leaves existing draft unchanged`() {
        assertEquals("typed  ", VoiceDraftMerger.merge("typed  ", "   "))
    }
}
