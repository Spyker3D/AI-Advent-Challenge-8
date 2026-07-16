package com.aiassistant.core.data.mapper

import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrivateVpsMapperTest {
    @Test fun normalizesBaseUrlAndAvoidsDuplicateApi() {
        assertEquals("http://example.com/", ChatSettings.normalizePrivateVpsBaseUrl(" http://example.com/api/ "))
        assertEquals("https://example.com/", ChatSettings.normalizePrivateVpsBaseUrl("https://example.com"))
        assertNull(ChatSettings.normalizePrivateVpsBaseUrl("example.com"))
    }

    @Test fun mapsHistoryOnceAndUsesVpsGenerationSettings() {
        val request = buildPrivateVpsRequest(
            listOf(
                Message("1", "rules", MessageRole.SYSTEM),
                Message("2", "", MessageRole.USER),
                Message("3", "Hello", MessageRole.USER),
                Message("4", "Hi", MessageRole.ASSISTANT)
            ),
            ChatSettings(privateVpsModel = "qwen2.5:3b", localTemperature = 0.2f, localMaxTokens = 700)
        )
        assertEquals("qwen2.5:3b", request.model)
        assertEquals(listOf("system", "user", "assistant"), request.messages.map { it.role })
        assertEquals(0.2, request.temperature!!, 0.0001)
        assertEquals(700, request.maxTokens)
        assertEquals(false, request.stream)
    }
}
