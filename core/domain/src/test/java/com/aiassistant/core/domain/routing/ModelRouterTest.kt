package com.aiassistant.core.domain.routing

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.Message
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ModelRouterTest {
    @Test fun `high confidence stays on small`() = runBlocking { assertSmall(0.95) }
    @Test fun `threshold confidence stays on small`() = runBlocking { assertSmall(0.80) }
    @Test fun `low confidence escalates`() = runBlocking { assertEscalates(decision(0.79), RoutingReason.LOW_CONFIDENCE) }
    @Test fun `model request escalates before low confidence`() = runBlocking { assertEscalates(decision(0.4, true), RoutingReason.MODEL_REQUESTED_ESCALATION) }
    @Test fun `invalid JSON escalates`() = runBlocking { assertEscalates("not json", RoutingReason.INVALID_JSON) }
    @Test fun `short factual answer stays on small`() = runBlocking {
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse(decision(0.95, answer = "Paris")))))
        assertFalse(ModelRouter(fake).route("Name the capital of France").getOrThrow().escalated)
    }
    @Test fun `small request uses deterministic structured options`() = runBlocking {
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse(decision(0.95)))))
        ModelRouter(fake).route("simple request").getOrThrow()
        assertEquals(0.0, fake.options.single()!!.temperature!!, 0.0)
        assertEquals(180, fake.options.single()!!.numPredict)
        assertTrue(fake.options.single()!!.jsonSchema != null)
    }
    @Test fun `small failure falls back`() = runBlocking {
        val fake = FakeLlmClient(mutableListOf(Result.failure(Exception("small")), Result.success(ChatResponse("large answer"))))
        val result = ModelRouter(fake).route("simple request").getOrThrow()
        assertEquals(RoutingReason.SMALL_MODEL_ERROR, result.escalationReason)
    }
    @Test fun `long request skips small`() = runBlocking { assertDirectLarge("x".repeat(401), RoutingReason.LONG_REQUEST) }
    @Test fun `complex marker skips small`() = runBlocking { assertDirectLarge("Investigate this race condition", RoutingReason.COMPLEX_REQUEST) }
    @Test fun `large failure is not successful`() = runBlocking {
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse("bad json")), Result.failure(Exception("large"))))
        val result = ModelRouter(fake).route("simple request")
        assertTrue(result.isFailure)
        assertEquals(RoutingReason.LARGE_MODEL_ERROR, (result.exceptionOrNull() as ModelRoutingException).reason)
    }
    @Test fun `cancellation is propagated without fallback`() {
        val fake = FakeLlmClient(mutableListOf(Result.failure(kotlinx.coroutines.CancellationException("cancelled"))))
        try {
            runBlocking { ModelRouter(fake).route("simple request") }
            fail("Expected CancellationException")
        } catch (_: kotlinx.coroutines.CancellationException) {
            assertEquals(1, fake.calls.size)
        }
    }
    @Test fun `blank request calls no model`() = runBlocking {
        val fake = FakeLlmClient(mutableListOf())
        assertTrue(ModelRouter(fake).route("  ").isFailure)
        assertTrue(fake.calls.isEmpty())
    }
    @Test fun `uses exact model names`() = runBlocking {
        assertEquals("llama3.2:3b", RoutingConfig.SMALL_MODEL)
        assertEquals("qwen2.5:7b-instruct", RoutingConfig.LARGE_MODEL)
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse(decision(0.79))), Result.success(ChatResponse("large answer"))))
        ModelRouter(fake).route("simple request").getOrThrow()
        assertEquals(listOf(RoutingConfig.SMALL_MODEL, RoutingConfig.LARGE_MODEL), fake.calls.map { it.model })
    }
    @Test fun `large receives original request not small draft`() = runBlocking {
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse(decision(0.79, answer = "untrusted draft answer"))), Result.success(ChatResponse("large answer"))))
        ModelRouter(fake).route("original request").getOrThrow()
        assertEquals("original request", fake.calls[1].messages.last().content)
        assertNotNull(fake.options[0]?.jsonSchema)
        assertNull(fake.options[1])
    }

    @Test fun `multiple possible causes without evidence escalates`() = runBlocking {
        assertEscalates(decision(0.92, ambiguity = Ambiguity.HIGH, sufficientContext = false), RoutingReason.MODEL_REQUESTED_ESCALATION)
    }
    @Test fun `contradictory requirements decision escalates`() = runBlocking {
        assertEscalates(decision(0.9, ambiguity = Ambiguity.MEDIUM), RoutingReason.MODEL_REQUESTED_ESCALATION)
    }
    @Test fun `incomplete question decision escalates without sufficient context`() = runBlocking {
        assertEscalates(decision(0.9, sufficientContext = false), RoutingReason.MODEL_REQUESTED_ESCALATION)
    }
    @Test fun `common knowledge with low ambiguity and sufficient context stays small`() = runBlocking { assertSmall(0.95) }
    @Test fun `simple translation with low ambiguity and sufficient context stays small`() = runBlocking { assertSmall(0.96) }
    @Test fun `architectural alternatives without criteria decision escalates`() = runBlocking {
        assertEscalates(decision(0.88, ambiguity = Ambiguity.MEDIUM), RoutingReason.MODEL_REQUESTED_ESCALATION)
    }
    @Test fun `precise diagnosis without evidence escalates`() = runBlocking {
        assertEscalates(decision(0.85, sufficientContext = false), RoutingReason.MODEL_REQUESTED_ESCALATION)
    }
    private suspend fun assertSmall(confidence: Double) {
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse(decision(confidence)))))
        val result = ModelRouter(fake).route("simple request").getOrThrow()
        assertFalse(result.escalated); assertEquals(RoutingConfig.SMALL_MODEL, result.finalModel); assertEquals(1, fake.calls.size)
    }
    private suspend fun assertEscalates(raw: String, reason: RoutingReason) {
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse(raw)), Result.success(ChatResponse("large final answer"))))
        val result = ModelRouter(fake).route("simple request").getOrThrow()
        assertTrue(result.escalated); assertEquals(reason, result.escalationReason)
    }
    private suspend fun assertDirectLarge(input: String, reason: RoutingReason) {
        val fake = FakeLlmClient(mutableListOf(Result.success(ChatResponse("large final answer"))))
        val result = ModelRouter(fake).route(input).getOrThrow()
        assertEquals(reason, result.escalationReason); assertEquals(1, fake.calls.size); assertEquals(RoutingConfig.LARGE_MODEL, fake.calls.single().model)
    }
    private fun decision(
        confidence: Double,
        escalation: Boolean = false,
        answer: String = "This answer is definitely long enough",
        ambiguity: Ambiguity = Ambiguity.LOW,
        sufficientContext: Boolean = true
    ) = """{"answer":"$answer","confidence":$confidence,"needs_escalation":$escalation,"ambiguity":"${ambiguity.name}","sufficient_context":$sufficientContext,"reason":"test reason"}"""
    private data class Call(val messages: List<Message>, val model: String?)
    private class FakeLlmClient(private val responses: MutableList<Result<ChatResponse>>) : LlmClient {
        val calls = mutableListOf<Call>()
        val options = mutableListOf<LlmRequestOptions?>()
        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> {
            calls += Call(messages, model); options += null; return responses.removeAt(0)
        }
        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?, options: LlmRequestOptions): Result<ChatResponse> {
            calls += Call(messages, model); this.options += options; return responses.removeAt(0)
        }
    }
}
