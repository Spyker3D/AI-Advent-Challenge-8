package com.aiassistant.core.data.microfirst

import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.repository.SettingsRepository
import com.aiassistant.core.network.api.OllamaApi
import com.aiassistant.core.network.dto.OllamaEmbedRequestDto
import com.aiassistant.core.network.dto.OllamaEmbedResponseDto
import com.aiassistant.core.network.dto.OllamaGenerateRequestDto
import com.aiassistant.core.network.dto.OllamaGenerateResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class OllamaEmbeddingClientTest {

    @Test
    fun `sends configured batch request and returns embeddings`() = runBlocking {
        val api = FakeOllamaApi(OllamaEmbedResponseDto(listOf(listOf(1f, 2f), listOf(3f, 4f))))
        val client = OllamaEmbeddingClient(FakeSettingsRepository("http://ollama.test"), api)

        val result = client.embed(listOf("alpha", "beta"), "embed-model")

        assertEquals(listOf(listOf(1f, 2f), listOf(3f, 4f)), result.getOrThrow())
        assertEquals(OllamaEmbedRequestDto("embed-model", listOf("alpha", "beta")), api.request)
    }

    @Test
    fun `empty input succeeds without calling api`() = runBlocking {
        val api = FakeOllamaApi(OllamaEmbedResponseDto(null))
        val result = OllamaEmbeddingClient(FakeSettingsRepository("http://ollama.test"), api)
            .embed(emptyList(), "embed-model")

        assertEquals(emptyList<List<Float>>(), result.getOrThrow())
        assertFalse(api.wasCalled)
    }

    @Test
    fun `rejects empty missing count dimensions and non finite responses`() = runBlocking {
        val invalid = listOf(
            null,
            emptyList(),
            listOf(listOf(1f)),
            listOf(emptyList(), emptyList()),
            listOf(listOf(1f), listOf(1f, 2f)),
            listOf(listOf(Float.NaN), listOf(1f)),
            listOf(listOf(Float.POSITIVE_INFINITY), listOf(1f))
        )

        invalid.forEach { embeddings ->
            val result = OllamaEmbeddingClient(
                FakeSettingsRepository("http://ollama.test"),
                FakeOllamaApi(OllamaEmbedResponseDto(embeddings))
            ).embed(listOf("alpha", "beta"), "embed-model")
            assertTrue("Expected failure for $embeddings", result.isFailure)
        }
    }

    @Test
    fun `converts http failure without exposing input text`() = runBlocking {
        val api = object : OllamaApi {
            override suspend fun embed(request: OllamaEmbedRequestDto): OllamaEmbedResponseDto =
                throw HttpException(Response.error<OllamaEmbedResponseDto>(429, "rate limit".toResponseBody()))

            override suspend fun generate(request: OllamaGenerateRequestDto): OllamaGenerateResponseDto =
                error("not used")
        }

        val result = OllamaEmbeddingClient(FakeSettingsRepository("http://ollama.test"), api)
            .embed(listOf("private user text"), "embed-model")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("429"))
        assertFalse(result.exceptionOrNull()?.message.orEmpty().contains("private user text"))
    }

    @Test
    fun `propagates cancellation`() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                val api = object : OllamaApi {
                    override suspend fun embed(request: OllamaEmbedRequestDto): OllamaEmbedResponseDto =
                        throw CancellationException("cancelled")

                    override suspend fun generate(request: OllamaGenerateRequestDto): OllamaGenerateResponseDto =
                        error("not used")
                }

                OllamaEmbeddingClient(FakeSettingsRepository("http://ollama.test"), api)
                    .embed(listOf("private user text"), "embed-model")
            }
        }
    }

    private class FakeOllamaApi(
        private val response: OllamaEmbedResponseDto
    ) : OllamaApi {
        var request: OllamaEmbedRequestDto? = null
        var wasCalled = false

        override suspend fun embed(request: OllamaEmbedRequestDto): OllamaEmbedResponseDto {
            wasCalled = true
            this.request = request
            return response
        }

        override suspend fun generate(request: OllamaGenerateRequestDto): OllamaGenerateResponseDto =
            error("not used")
    }

    private class FakeSettingsRepository(baseUrl: String) : SettingsRepository {
        private val settings = ChatSettings(localBaseUrl = baseUrl)
        override fun getChatSettings(): Flow<ChatSettings> = flowOf(settings)
        override suspend fun saveChatSettings(settings: ChatSettings) = Unit
    }
}
