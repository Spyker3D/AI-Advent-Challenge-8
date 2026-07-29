package com.aiassistant.core.data.client

import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.repository.SettingsRepository
import com.aiassistant.core.network.api.OllamaApiFactory
import com.aiassistant.core.network.api.OpenAiApi
import com.aiassistant.core.network.api.PrivateVpsApi
import com.aiassistant.core.network.interceptor.PrivateVpsCredentials
import com.google.gson.Gson
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmClientImplOllamaTest {

    @Test
    fun `invalid base URL returns controlled error`() = runBlocking {
        val client = createClient(ChatSettings(provider = AiProvider.LOCAL_OLLAMA, localBaseUrl = "://invalid"))

        val result = client.sendChat(userMessages(), maxTokens = null, model = null)

        assertTrue(result.isFailure)
        assertEquals("Invalid Ollama Base URL: ://invalid", result.exceptionOrNull()?.message)
    }

    @Test
    fun `connection refusal returns actionable error without secrets`() = runBlocking {
        val server = MockWebServer()
        server.start()
        val unavailableUrl = server.url("/").toString()
        server.shutdown()
        val client = createClient(ChatSettings(provider = AiProvider.LOCAL_OLLAMA, localBaseUrl = unavailableUrl))

        val result = client.sendChat(userMessages(), maxTokens = null, model = null)
        val message = result.exceptionOrNull()?.message.orEmpty()

        assertTrue(result.isFailure)
        assertTrue(message.contains("Ollama", ignoreCase = true) || message.contains("local LLM", ignoreCase = true))
        assertTrue(message.contains(unavailableUrl))
        assertFalse(message.contains("api key", ignoreCase = true))
    }

    @Test
    fun `blank model response returns controlled error`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"response":"   ","done":true}"""))
        server.start()
        try {
            val client = createClient(
                ChatSettings(provider = AiProvider.LOCAL_OLLAMA, localBaseUrl = server.url("/").toString())
            )

            val result = client.sendChat(userMessages(), maxTokens = null, model = null)

            assertTrue(result.isFailure)
            assertEquals("Empty response from local LLM", result.exceptionOrNull()?.message)
        } finally {
            server.shutdown()
        }
    }

    private fun createClient(settings: ChatSettings): LlmClientImpl = LlmClientImpl(
        openAiApi = unusedApi(),
        privateVpsApi = unusedApi(),
        privateVpsCredentials = PrivateVpsCredentials(),
        ollamaApiFactory = OllamaApiFactory(OkHttpClient(), Gson()),
        settingsRepository = FakeSettingsRepository(settings),
        apiConfig = ApiConfig(openAiApiKey = "")
    )

    private fun userMessages() = listOf(
        Message(id = "message-1", content = "Hello", role = MessageRole.USER)
    )

    private inline fun <reified T> unusedApi(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java)
    ) { _, method, _ -> error("Unexpected ${method.name} call") } as T

    private class FakeSettingsRepository(settings: ChatSettings) : SettingsRepository {
        private val values = flowOf(settings)

        override fun getChatSettings(): Flow<ChatSettings> = values

        override suspend fun saveChatSettings(settings: ChatSettings) = Unit
    }
}