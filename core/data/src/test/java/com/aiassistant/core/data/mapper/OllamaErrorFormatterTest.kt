package com.aiassistant.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class OllamaErrorFormatterTest {

    @Test
    fun `preserves invalid URL and empty response messages`() {
        assertEquals(
            "Invalid Ollama Base URL: ://invalid",
            OllamaErrorFormatter.invalidBaseUrl("://invalid")
        )
        assertEquals("Empty response from local LLM", OllamaErrorFormatter.emptyResponse())
    }

    @Test
    fun `preserves connection and timeout guidance`() {
        assertEquals(
            "Не удалось подключиться к локальной LLM.\n" +
                "Проверь, что Ollama запущена и доступна по http://localhost:11434/",
            OllamaErrorFormatter.connectionError("http://localhost:11434/")
        )
        assertEquals(
            "Local LLM request timed out. Check that Ollama is running and the model is responding.",
            OllamaErrorFormatter.timeout()
        )
    }

    @Test
    fun `formats model not found and other HTTP errors`() {
        assertEquals(
            "Модель qwen:test не установлена.\nВыполните:\nollama pull qwen:test",
            OllamaErrorFormatter.httpError(404, "Not Found", "qwen:test")
        )
        assertEquals(
            "Ollama HTTP 500: Server Error",
            OllamaErrorFormatter.httpError(500, "Server Error", "qwen:test")
        )
    }
}