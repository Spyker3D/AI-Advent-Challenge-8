package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.inference.IncidentCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroFirstInferenceEngineTest {
    @Test
    fun `confident centroid match is handled without large model`() = runBlocking {
        val embeddings = FakeEmbeddingClient(queryVector = axis(0))
        val llm = FakeLlmClient()
        val engine = MicroFirstInferenceEngine(embeddings, FakePrototypeProvider(), llm)

        val result = engine.execute("РўРµРєСѓС‰РёР№ Р·Р°РїСЂРѕСЃ").getOrThrow()

        assertTrue(result.handledByMicro)
        assertFalse(result.fallbackUsed)
        assertEquals(MicroResponseFormatter.format(IncidentCategory.NETWORK_UNAVAILABLE), result.finalText)
        assertEquals(IncidentCategory.NETWORK_UNAVAILABLE, result.microResult?.label)
        assertEquals(MicroStatus.OK, result.microResult?.status)
        assertEquals(0, result.largeLlmCalls)
        assertNull(result.fallbackReason)
        assertTrue(embeddings.models.all { it == MicroFirstConfig.MICRO_MODEL })
        assertEquals(0, llm.calls)
    }

    @Test
    fun `category score uses maximum prototype cosine`() = runBlocking {
        val vectors = listOf(
            axis(0), axis(0).map { -it },
            axis(1), axis(1),
            axis(2), axis(2),
            axis(3), axis(3),
            axis(4), axis(4)
        )
        val engine = MicroFirstInferenceEngine(
            FakeEmbeddingClient(axis(0), vectors),
            FakePrototypeProvider(prototypesPerCategory = 2),
            FakeLlmClient()
        )

        val result = engine.execute("request").getOrThrow()

        assertTrue(result.handledByMicro)
        assertEquals(IncidentCategory.NETWORK_UNAVAILABLE, result.microResult?.label)
        assertEquals(1.0, result.microResult?.score ?: 0.0, 0.000001)
    }

    @Test
    fun `low score uses strict local fallback with current input only`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(validFallback()))
        val engine = MicroFirstInferenceEngine(
            FakeEmbeddingClient(queryVector = List(5) { 1f }), FakePrototypeProvider(), llm
        )

        val result = engine.execute("РўРѕР»СЊРєРѕ СЌС‚РѕС‚ Р·Р°РїСЂРѕСЃ").getOrThrow()

        assertEquals(MicroFallbackReason.LOW_SCORE, result.fallbackReason)
        assertEquals(MicroStatus.UNSURE, result.microResult?.status)
        assertNull(result.microResult?.label)
        assertTrue(result.microResult!!.rankedCandidates.isNotEmpty())
        assertTrue(result.fallbackUsed)
        assertEquals(1, result.largeLlmCalls)
        assertEquals(MicroFirstConfig.FALLBACK_MODEL, result.fallbackModel)
        assertEquals(MicroResponseFormatter.format(IncidentCategory.NETWORK_UNAVAILABLE), result.finalText)
        assertEquals("РўРѕР»СЊРєРѕ СЌС‚РѕС‚ Р·Р°РїСЂРѕСЃ", llm.requests.single().single { it.role.name == "USER" }.content)
        val options = llm.options.single()
        assertEquals(0.0, options.temperature)
        assertEquals(false, options.stream)
        assertEquals(AiProvider.LOCAL_OLLAMA, options.requiredProvider)
        assertEquals(MicroFallbackContract.SCHEMA, options.jsonSchema)
    }

    @Test
    fun `low margin uses fallback even above score threshold`() = runBlocking {
        val query = listOf(1f, 0f, 0f, 0f, 0f)
        val client = FakeEmbeddingClient(queryVector = query, prototypeVectors = listOf(
            listOf(1f, 0f, 0f, 0f, 0f),
            listOf(0.999f, 0.045f, 0f, 0f, 0f),
            axis(2), axis(3), axis(4)
        ))
        val engine = MicroFirstInferenceEngine(client, FakePrototypeProvider(), FakeLlmClient(mutableListOf(validFallback())))

        val result = engine.execute("Р—Р°РїСЂРѕСЃ").getOrThrow()

        assertEquals(MicroFallbackReason.LOW_MARGIN, result.fallbackReason)
        assertTrue(result.microResult!!.score >= MicroFirstConfig.MIN_TOP_SCORE)
        assertTrue(result.microResult!!.margin < MicroFirstConfig.MIN_MARGIN)
    }

    @Test
    fun `invalid prototype vector falls back and failed initialization is retryable`() = runBlocking {
        val embedding = FakeEmbeddingClient(queryVector = axis(0), invalidFirstPrototypeBatch = true)
        val llm = FakeLlmClient(mutableListOf(validFallback()))
        val engine = MicroFirstInferenceEngine(embedding, FakePrototypeProvider(), llm)

        val first = engine.execute("РџРµСЂРІС‹Р№").getOrThrow()
        val second = engine.execute("Р’С‚РѕСЂРѕР№").getOrThrow()

        assertEquals(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR, first.fallbackReason)
        assertTrue(second.handledByMicro)
        assertEquals(2, embedding.prototypeBatchCalls)
    }

    @Test
    fun `prototype centroids are initialized once under concurrent requests and can be cleared`() = runBlocking {
        val embedding = FakeEmbeddingClient(queryVector = axis(0))
        val provider = FakePrototypeProvider()
        val engine = MicroFirstInferenceEngine(embedding, provider, FakeLlmClient())

        listOf("a", "b", "c").map { async { engine.execute(it).getOrThrow() } }.awaitAll()
        assertEquals(1, provider.calls)
        assertEquals(1, embedding.prototypeBatchCalls)

        engine.clearCacheForTests()
        engine.execute("d").getOrThrow()
        assertEquals(2, provider.calls)
        assertEquals(2, embedding.prototypeBatchCalls)
    }

    @Test
    fun `validation failure gets exactly one correction retry`() = runBlocking {
        val invalidCategory = validFallback().replace("NETWORK_UNAVAILABLE", "NOT_A_CATEGORY")
        val llm = FakeLlmClient(mutableListOf(invalidCategory, validFallback()))
        val engine = MicroFirstInferenceEngine(FakeEmbeddingClient(List(5) { 1f }), FakePrototypeProvider(), llm)

        val result = engine.execute("Р—Р°РїСЂРѕСЃ").getOrThrow()

        assertEquals(2, result.largeLlmCalls)
        assertEquals(2, llm.calls)
        assertTrue(llm.requests[1].first { it.role.name == "SYSTEM" }.content.contains(MicroFallbackContract.CORRECTION))
    }

    @Test
    fun `format failure gets one correction retry and can recover`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf("not json", validFallback()))
        val engine = MicroFirstInferenceEngine(FakeEmbeddingClient(List(5) { 1f }), FakePrototypeProvider(), llm)

        assertTrue(engine.execute("request").isSuccess)
        assertEquals(2, llm.calls)
    }

    @Test
    fun `measured target margin 052497 remains safe fallback`() = runBlocking {
        fun cosineVector(cosine: Double) = listOf(
            cosine.toFloat(),
            kotlin.math.sqrt(1.0 - cosine * cosine).toFloat(),
            0f,
            0f,
            0f
        )
        val vectors = listOf(cosineVector(0.8), cosineVector(0.747503), axis(2), axis(3), axis(4))
        val engine = MicroFirstInferenceEngine(
            FakeEmbeddingClient(axis(0), vectors),
            FakePrototypeProvider(),
            FakeLlmClient(mutableListOf(validFallback()))
        )

        val result = engine.execute("target").getOrThrow()

        assertTrue(result.fallbackUsed)
        assertEquals(MicroFallbackReason.LOW_MARGIN, result.fallbackReason)
        assertEquals(0.052497, result.microResult?.margin ?: 0.0, 0.00001)
    }

    @Test
    fun `timeout category with rate prose is accepted without semantic retry`() = runBlocking {
        val response = """{"category":"OPENAI_TIMEOUT","confidence":0.9,"reason":"Превышен лимит запросов и истекло время ожидания"}"""
        val llm = FakeLlmClient(mutableListOf(response))
        val engine = MicroFirstInferenceEngine(FakeEmbeddingClient(List(5) { 1f }), FakePrototypeProvider(), llm)

        val result = engine.execute("request").getOrThrow()

        assertEquals(1, result.largeLlmCalls)
        assertEquals(1, llm.calls)
        assertEquals(MicroResponseFormatter.format(IncidentCategory.OPENAI_TIMEOUT), result.finalText)
    }

    @Test
    fun `embedding cancellation propagates and never calls fallback`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(validFallback()))
        val embedding = object : EmbeddingClient {
            override suspend fun embed(texts: List<String>, model: String): Result<List<List<Float>>> {
                throw CancellationException("cancelled")
            }
        }
        val engine = MicroFirstInferenceEngine(embedding, FakePrototypeProvider(), llm)

        var cancelled = false
        try {
            engine.execute("Р—Р°РїСЂРѕСЃ")
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertEquals(0, llm.calls)
    }

    @Test
    fun `query embedding failure and invalid query vector have distinct reasons`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(validFallback(), validFallback()))
        val failingQuery = object : EmbeddingClient {
            var calls = 0
            override suspend fun embed(texts: List<String>, model: String): Result<List<List<Float>>> =
                if (++calls == 1) Result.success((0..4).map(::axis)) else Result.failure(IllegalStateException())
        }
        val failed = MicroFirstInferenceEngine(failingQuery, FakePrototypeProvider(), llm).execute(llm.toString()).getOrThrow()
        val invalid = MicroFirstInferenceEngine(FakeEmbeddingClient(emptyList()), FakePrototypeProvider(), llm).execute(llm.toString()).getOrThrow()

        assertEquals(MicroFallbackReason.EMBEDDING_ERROR, failed.fallbackReason)
        assertEquals(MicroFallbackReason.INVALID_VECTOR, invalid.fallbackReason)
    }

    @Test
    fun `formatter supports five concrete categories and rejects ambiguous`() {
        assertTrue(IncidentCategory.entries.all { MicroResponseFormatter.format(it).isNotBlank() })
        assertTrue(MicroResponseFormatter.format(IncidentCategory.AMBIGUOUS).isNotBlank())
    }

    private class FakePrototypeProvider(
        private val prototypesPerCategory: Int = 1
    ) : MicroPrototypeProvider {
        var calls = 0
        override suspend fun loadPrototypes(): Result<Map<IncidentCategory, List<String>>> {
            calls++
            return Result.success(IncidentCategory.entries.filter { it != IncidentCategory.AMBIGUOUS }
                .associateWith { category ->
                    List(prototypesPerCategory) { index -> "prototype-${category.name}-$index" }
                })
        }
    }

    private class FakeEmbeddingClient(
        private val queryVector: List<Float>,
        private val prototypeVectors: List<List<Float>> = (0..4).map(::axis),
        private var invalidFirstPrototypeBatch: Boolean = false
    ) : EmbeddingClient {
        val models = mutableListOf<String>()
        var prototypeBatchCalls = 0
        override suspend fun embed(texts: List<String>, model: String): Result<List<List<Float>>> {
            models += model
            return if (texts.size > 1) {
                prototypeBatchCalls++
                if (invalidFirstPrototypeBatch) {
                    invalidFirstPrototypeBatch = false
                    Result.success(listOf(emptyList()))
                } else Result.success(prototypeVectors)
            } else Result.success(listOf(queryVector))
        }
    }

    private class FakeLlmClient(private val responses: MutableList<String> = mutableListOf()) : LlmClient {
        var calls = 0
        val requests = mutableListOf<List<Message>>()
        val options = mutableListOf<LlmRequestOptions>()
        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> =
            error("Options overload expected")
        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?, options: LlmRequestOptions): Result<ChatResponse> {
            calls++
            requests += messages
            this.options += options
            return Result.success(ChatResponse(responses.removeAt(0)))
        }
    }

    private companion object {
        fun axis(index: Int) = List(5) { if (it == index) 1f else 0f }
        fun validFallback() = """{"category":"NETWORK_UNAVAILABLE","confidence":0.9,"reason":"Нет сети"}"""
    }
}
