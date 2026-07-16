package com.aiassistant.core.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSettingsTest {

    @Test
    fun defaultLocalModelUsesQwen25SevenB() {
        assertEquals("qwen2.5:7b-instruct", ChatSettings().localModel)
        assertEquals("qwen2.5:7b-instruct", ChatSettings.DEFAULT_LOCAL_MODEL)
    }

    @Test
    fun legacyOpenRouterModelNamesMigrateToOpenAiDefault() {
        assertEquals("gpt-4.1-mini", ChatSettings.DEFAULT_OPENAI_MODEL)
        assertEquals("gpt-4.1-mini", ChatSettings.normalizeOpenAiModel("openai/gpt-4o-mini"))
        assertEquals("gpt-4.1-mini", ChatSettings.normalizeOpenAiModel("openai/gpt-4.1-mini"))
        assertEquals("gpt-4.1-mini", ChatSettings.normalizeOpenAiModel("gpt-4o-mini"))
    }

    @Test
    fun invalidLocalGenerationValuesUseSafeDefaults() {
        assertEquals(0.2f, ChatSettings.safeLocalTemperature(9f))
        assertEquals(700, ChatSettings.safeLocalMaxTokens(12))
        assertEquals(8192, ChatSettings.safeLocalContextWindow(3000))
        assertEquals(0.9f, ChatSettings.safeLocalTopP(0f))
        assertEquals(1.1f, ChatSettings.safeLocalRepeatPenalty(3f))
    }

    @Test
    fun q5AndCustomOllamaTagsArePreservedExactly() {
        assertEquals(
            "qwen2.5:7b-instruct-q5_K_M",
            ChatSettings.normalizeLocalModelTag("qwen2.5:7b-instruct-q5_K_M")
        )
        assertEquals("my-registry/custom-model:latest", ChatSettings.normalizeLocalModelTag("my-registry/custom-model:latest"))
    }
}
