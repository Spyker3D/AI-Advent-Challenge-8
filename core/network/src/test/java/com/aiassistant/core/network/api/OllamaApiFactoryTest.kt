package com.aiassistant.core.network.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OllamaApiFactoryTest {
    private val factory = OllamaApiFactory(OkHttpClient(), Gson())

    @Test
    fun `normalizes whitespace and missing trailing slash`() {
        assertEquals(
            "http://localhost:11434/",
            factory.normalizeBaseUrl("  http://localhost:11434  ")
        )
    }

    @Test
    fun `collapses trailing slashes`() {
        assertEquals(
            "http://localhost:11434/",
            factory.normalizeBaseUrl("http://localhost:11434///")
        )
    }

    @Test
    fun `preserves a valid normalized URL`() {
        assertEquals(
            "https://ollama.example/api/",
            factory.normalizeBaseUrl("https://ollama.example/api/")
        )
    }

    @Test
    fun `rejects invalid URL`() {
        assertThrows(IllegalArgumentException::class.java) {
            factory.normalizeBaseUrl("://invalid")
        }
    }
}