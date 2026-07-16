package com.aiassistant.core.network

import com.aiassistant.core.network.api.PrivateVpsApi
import com.aiassistant.core.network.dto.PrivateVpsChatRequestDto
import com.aiassistant.core.network.dto.PrivateVpsMessageDto
import com.aiassistant.core.network.interceptor.PrivateVpsAuthInterceptor
import com.aiassistant.core.network.interceptor.PrivateVpsCredentials
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PrivateVpsIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var api: PrivateVpsApi

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        val credentials = PrivateVpsCredentials().apply { apiKey = "test-key" }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(PrivateVpsAuthInterceptor(credentials)).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(PrivateVpsApi::class.java)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun sendsExpectedChatCompletionAndMapsResponse() {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
            {"id":"chatcmpl-test","model":"qwen2.5:3b","choices":[{"index":0,
            "message":{"role":"assistant","content":"Private VPS model is working."},
            "finish_reason":"stop"}],"usage":{"input_tokens":10,"output_tokens":6,"total_tokens":16}}
        """.trimIndent()))

        val response = kotlinx.coroutines.runBlocking {
            api.createChatCompletion(server.url("/api/chat/completions").toString(),
                PrivateVpsChatRequestDto("qwen2.5:3b", listOf(PrivateVpsMessageDto("user", "Hello")), 0.2, 700, false))
        }

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/chat/completions", recorded.path)
        assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
        val json = Gson().fromJson(recorded.body.readUtf8(), Map::class.java)
        assertEquals("qwen2.5:3b", json["model"])
        assertEquals(700.0, json["max_tokens"])
        assertEquals(false, json["stream"])
        assertEquals("Private VPS model is working.", response.outputText())
        assertEquals(6, response.usage?.effectiveOutputTokens())
    }

    @Test fun emptyKeyIsRejectedBeforeNetworkRequest() {
        val credentials = PrivateVpsCredentials()
        val client = OkHttpClient.Builder().addInterceptor(PrivateVpsAuthInterceptor(credentials)).build()
        val failure = runCatching { client.newCall(okhttp3.Request.Builder().url(server.url("/")).build()).execute() }
        assertTrue(failure.isFailure)
        assertFalse(server.requestCount > 0)
    }
}
