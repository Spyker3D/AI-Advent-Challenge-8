package com.aiassistant.core.data.mapper

import com.aiassistant.core.domain.entity.ChatSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class OllamaOptionsMapperTest {
    @Test
    fun `maps all persisted local settings to Ollama options`() {
        val options = ChatSettings(
            localTemperature = 0.55f,
            localMaxTokens = 1024,
            localContextWindow = 16384,
            localTopP = 0.75f,
            localRepeatPenalty = 1.25f,
            localSeed = 123
        ).toOllamaOptionsDto()

        assertEquals(0.55, options.temperature!!, 0.0001)
        assertEquals(1024, options.numPredict)
        assertEquals(16384, options.numCtx)
        assertEquals(0.75, options.topP!!, 0.0001)
        assertEquals(1.25, options.repeatPenalty!!, 0.0001)
        assertEquals(123, options.seed)
    }

    @Test
    fun `replaces invalid values with safe defaults`() {
        val options = ChatSettings(localTemperature = 9f, localMaxTokens = 1,
            localContextWindow = 3, localTopP = 0f, localRepeatPenalty = 9f).toOllamaOptionsDto()
        assertEquals(0.2, options.temperature!!, 0.0001)
        assertEquals(700, options.numPredict)
        assertEquals(8192, options.numCtx)
        assertEquals(0.9, options.topP!!, 0.0001)
        assertEquals(1.1, options.repeatPenalty!!, 0.0001)
    }
}
