package com.aiassistant.core.network

import com.aiassistant.core.network.dto.OpenAiOutputContentDto
import com.aiassistant.core.network.dto.OpenAiOutputItemDto
import com.aiassistant.core.network.dto.OpenAiResponseDto
import com.aiassistant.core.network.dto.OpenAiResponseRequestDto
import com.aiassistant.core.network.dto.OpenAiInputMessageDto
import com.google.gson.Gson
import com.aiassistant.core.network.interceptor.OpenAiAuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OpenAiIntegrationTest {
    @Test
    fun `auth interceptor adds bearer header without leaking key to logs`() {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody("{}")) }
        val logs = mutableListOf<String>()
        val logger = HttpLoggingInterceptor { logs += it }.apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(OpenAiAuthInterceptor("test-api-key"))
            .addInterceptor(logger)
            .build()

        client.newCall(Request.Builder().url(server.url("v1/responses")).build()).execute().close()

        assertEquals("Bearer test-api-key", server.takeRequest().getHeader("Authorization"))
        assertFalse(logs.joinToString("\n").contains("test-api-key"))
        server.shutdown()
    }

    @Test
    fun `extracts only output text and treats absent text as empty`() {
        val response = OpenAiResponseDto(null, null, listOf(OpenAiOutputItemDto(
            "message", "assistant", listOf(
                OpenAiOutputContentDto("reasoning", "hidden"),
                OpenAiOutputContentDto("output_text", "answer")
            )
        )), null)

        assertEquals("answer", response.outputText())
        assertEquals("", response.copy(output = emptyList()).outputText())
    }

    @Test
    fun `request JSON uses direct OpenAI model name`() {
        val json = Gson().toJson(OpenAiResponseRequestDto(
            model = "gpt-4.1-mini",
            input = listOf(OpenAiInputMessageDto("user", "hello"))
        ))

        assertFalse(json.contains("openai/"))
        assertEquals("gpt-4.1-mini", Gson().fromJson(json, Map::class.java)["model"])
    }
}
