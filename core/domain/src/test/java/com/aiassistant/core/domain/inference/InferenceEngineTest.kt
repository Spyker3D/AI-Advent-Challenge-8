package com.aiassistant.core.domain.inference

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.AiResponseMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InferenceEngineTest {
    @Test
    fun `monolithic mode returns parsed result with configured structured request`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(Outcome.Return(Result.success(ChatResponse(monolithicJson)))))
        val result = InferenceEngine(llm).execute("incident", InferenceMode.MONOLITHIC).getOrThrow()

        assertTrue(result.finalText.contains(IncidentAction.RETRY_REQUEST.userFacingText()))
        assertEquals(IncidentCategory.OPENAI_TIMEOUT, result.debugMetadata.decision?.category)
        assertEquals(InferenceConfig.MONOLITHIC_MODEL, llm.calls.single().model)
        assertEquals(InferenceSchemas.MONOLITHIC, llm.calls.single().options.jsonSchema)
        assertEquals(0.0, llm.calls.single().options.temperature)
        assertEquals(AiProvider.LOCAL_OLLAMA, llm.calls.single().options.requiredProvider)
        assertEquals(StageStatus.OK, result.debugMetadata.stageMetadata.single().status)
        assertEquals(null, result.debugMetadata.stageMetadata.single().error)
    }

    @Test
    fun `multi stage mode calls configured stages and returns successful metadata`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(normalizationJson))),
            Outcome.Return(Result.success(ChatResponse(decisionJson))),
            Outcome.Return(Result.success(ChatResponse(presentationJson)))
        ))
        val result = InferenceEngine(llm).execute("incident", InferenceMode.MULTI_STAGE).getOrThrow()

        assertEquals(3, llm.calls.size)
        assertEquals(listOf(InferenceConfig.NORMALIZATION_MODEL, InferenceConfig.DECISION_MODEL, InferenceConfig.PRESENTATION_MODEL), llm.calls.map { it.model })
        assertEquals(listOf(InferenceSchemas.NORMALIZATION, InferenceSchemas.DECISION, InferenceSchemas.PRESENTATION), llm.calls.map { it.options.jsonSchema })
        assertTrue(result.debugMetadata.formatCompliant)
        assertEquals(3, result.debugMetadata.totalModelCalls)
        assertTrue(result.debugMetadata.stageMetadata.all { it.status == StageStatus.OK })
        assertTrue(result.debugMetadata.stageMetadata.all { it.error == null })
        assertTrue(llm.calls.all { it.options.requiredProvider == AiProvider.LOCAL_OLLAMA })
    }

    @Test
    fun `alternative exclusion remains supported evidence for selected category`() = runBlocking {
        val normalized = """{"observed_facts":["remote service rejected request because rate was exceeded","connectivity is available"],"normalized_summary":"remote service rejection while connectivity remains available"}"""
        val decision = """{"category":"OPENAI_RATE_LIMIT","severity":"MEDIUM","action":"RETRY_WITH_BACKOFF","confidence":0.9,"evidence_state":"SUPPORTED","supporting_evidence":["remote service rejected request because rate was exceeded"],"contradicting_evidence":[]}"""
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(normalized))),
            Outcome.Return(Result.success(ChatResponse(decision))),
            Outcome.Return(Result.success(ChatResponse(presentationJson(IncidentAction.RETRY_WITH_BACKOFF))))
        ))

        val result = InferenceEngine(llm).execute("HTTP 429 + интернет работает", InferenceMode.MULTI_STAGE).getOrThrow()

        assertEquals(3, llm.calls.size)
        assertEquals(InferenceParsers.normalizedJson(NormalizedIncident(listOf("remote service rejected request because rate was exceeded", "connectivity is available"), "remote service rejection while connectivity remains available")), llm.calls[1].messages.last().content)
        assertEquals(IncidentCategory.OPENAI_RATE_LIMIT, result.debugMetadata.decision?.category)
        assertEquals(IncidentAction.RETRY_WITH_BACKOFF, result.debugMetadata.decision?.action)
        assertEquals(EvidenceState.SUPPORTED, result.debugMetadata.decision?.evidenceState)
        assertTrue(result.debugMetadata.decision?.contradictingEvidence?.isEmpty() == true)
    }

    @Test
    fun `rate limit sample has the same Russian presentation in both modes`() = runBlocking {
        val title = russian(1055, 1088, 1077, 1074, 1099, 1096, 1077, 1085, 32, 1083, 1080, 1084, 1080, 1090, 32, 1079, 1072, 1087, 1088, 1086, 1089, 1086, 1074)
        val message = russian(1055, 1086, 1076, 1086, 1078, 1076, 1080, 1090, 1077, 32, 1085, 1077, 1084, 1085, 1086, 1075, 1086, 32, 1080, 32, 1087, 1086, 1074, 1090, 1086, 1088, 1080, 1090, 1077, 32, 1079, 1072, 1087, 1088, 1086, 1089, 46)
        val action = IncidentAction.RETRY_WITH_BACKOFF.userFacingText()
        val expected = "$title\n\n$message\n\n$action"
        val decision = """{"category":"OPENAI_RATE_LIMIT","severity":"MEDIUM","action":"RETRY_WITH_BACKOFF","confidence":0.8567,"evidence_state":"SUPPORTED","supporting_evidence":["server rate limit"],"contradicting_evidence":[]}"""
        val presentation = """{"title":"$title","message":"$message","user_action":"$action"}"""
        val monolithic = """{"normalized_summary":"server rate limit","category":"OPENAI_RATE_LIMIT","severity":"MEDIUM","action":"RETRY_WITH_BACKOFF","confidence":0.8567,"evidence_state":"SUPPORTED","supporting_evidence":["server rate limit"],"contradicting_evidence":[],"title":"$title","message":"$message","user_action":"$action"}"""

        val multiClient = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse("""{"observed_facts":["server rate limit"],"normalized_summary":"server rate limit"}"""))),
            Outcome.Return(Result.success(ChatResponse(decision))),
            Outcome.Return(Result.success(ChatResponse(presentation)))
        ))
        val monoClient = FakeLlmClient(mutableListOf(Outcome.Return(Result.success(ChatResponse(monolithic)))))

        val multi = InferenceEngine(multiClient).execute("HTTP 429 Too Many Requests. Internet works.", InferenceMode.MULTI_STAGE).getOrThrow()
        val mono = InferenceEngine(monoClient).execute("HTTP 429 Too Many Requests. Internet works.", InferenceMode.MONOLITHIC).getOrThrow()

        listOf(multi, mono).forEach { result ->
            assertEquals(expected, result.finalText)
            assertEquals(IncidentCategory.OPENAI_RATE_LIMIT, result.debugMetadata.decision?.category)
            assertEquals(IncidentAction.RETRY_WITH_BACKOFF, result.debugMetadata.decision?.action)
        }
        assertEquals(3, multiClient.calls.size)
        assertEquals(1, monoClient.calls.size)
    }

    @Test
    fun `normalization failure skips later stages and preserves null message safely`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(Outcome.Return(Result.failure(IllegalStateException()))))
        val failure = InferenceEngine(llm).execute("incident", InferenceMode.MULTI_STAGE).exceptionOrNull()

        assertTrue(failure is InferencePipelineException)
        failure as InferencePipelineException
        assertEquals("IllegalStateException", failure.message)
        assertEquals(listOf(StageStatus.MODEL_ERROR, StageStatus.SKIPPED, StageStatus.SKIPPED), failure.debugMetadata.stageMetadata.map { it.status })
        assertEquals(1, llm.calls.size)
    }

    @Test
    fun `decision parse failure skips presentation`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(normalizationJson))),
            Outcome.Return(Result.success(ChatResponse("""{"category":"OPENAI_TIMEOUT"}"""))),
            Outcome.Return(Result.success(ChatResponse("""{"category":"OPENAI_TIMEOUT"}""")))
        ))
        val failure = InferenceEngine(llm).execute("incident", InferenceMode.MULTI_STAGE).exceptionOrNull() as InferencePipelineException

        assertEquals(listOf(StageStatus.OK, StageStatus.VALIDATION_ERROR, StageStatus.SKIPPED), failure.debugMetadata.stageMetadata.map { it.status })
        assertEquals(3, llm.calls.size)
    }

    @Test
    fun `presentation failure returns deterministic fallback and failed compliance metadata`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(normalizationJson))),
            Outcome.Return(Result.success(ChatResponse(decisionJson))),
            Outcome.Return(Result.success(ChatResponse("not json")))
        ))
        val result = InferenceEngine(llm).execute("incident", InferenceMode.MULTI_STAGE).getOrThrow()

        assertFalse(result.debugMetadata.formatCompliant)
        assertEquals(StageStatus.FORMAT_ERROR, result.debugMetadata.stageMetadata.last().status)
        assertTrue(result.finalText.isNotBlank())
        assertEquals(IncidentCategory.OPENAI_TIMEOUT, result.debugMetadata.decision?.category)
    }

    @Test
    fun `deterministic fallback uses the exact label for every action`() = runBlocking {
        IncidentCategory.entries.forEach { category ->
            val action = category.requiredAction()
            val ambiguous = category == IncidentCategory.AMBIGUOUS
            val evidenceState = if (ambiguous) EvidenceState.INSUFFICIENT else EvidenceState.SUPPORTED
            val evidence = if (ambiguous) emptyList() else listOf("explicit fact")
            val supporting = com.google.gson.Gson().toJson(evidence)
            val decision = """{"category":"${category.name}","severity":"MEDIUM","action":"${action.name}","confidence":0.8,"evidence_state":"${evidenceState.name}","supporting_evidence":$supporting,"contradicting_evidence":[]}"""
            val llm = FakeLlmClient(mutableListOf(
                Outcome.Return(Result.success(ChatResponse(normalizationJson))),
                Outcome.Return(Result.success(ChatResponse(decision))),
                Outcome.Return(Result.success(ChatResponse("not json")))
            ))
            val result = InferenceEngine(llm).execute("incident", InferenceMode.MULTI_STAGE).getOrThrow()

            assertTrue(result.finalText.endsWith(action.userFacingText()))
            assertTrue(IncidentAction.entries.none { result.finalText.contains(it.name) })
            assertEquals(category, result.debugMetadata.decision?.category)
            assertEquals(action, result.debugMetadata.decision?.action)
            assertFalse(result.debugMetadata.formatCompliant)
        }
    }

    @Test
    fun `validation error gets one neutral correction retry with identical request contract`() = runBlocking {
        val invalid = decisionJson.replace("\"confidence\":0.9", "\"confidence\":85")
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(normalizationJson))),
            Outcome.Return(Result.success(ChatResponse(invalid, metadata = tokenMetadata(2, 3)))),
            Outcome.Return(Result.success(ChatResponse(decisionJson, metadata = tokenMetadata(5, 7)))),
            Outcome.Return(Result.success(ChatResponse(presentationJson)))
        ))

        val result = InferenceEngine(llm).execute("original incident", InferenceMode.MULTI_STAGE).getOrThrow()

        assertEquals(4, result.debugMetadata.totalModelCalls)
        assertEquals(7, result.debugMetadata.stageMetadata[1].promptTokens)
        assertEquals(10, result.debugMetadata.stageMetadata[1].completionTokens)
        assertEquals(llm.calls[1].messages.last().content, llm.calls[2].messages.last().content)
        assertEquals(llm.calls[1].options, llm.calls[2].options)
        assertTrue(llm.calls[2].messages.first().content.contains(MultiStagePrompts.VALIDATION_CORRECTION))
        assertFalse(llm.calls[2].messages.first().content.contains(invalid))
        assertEquals(InferenceParsers.normalizedJson(NormalizedIncident(listOf("request exceeded allowed response time"), "timeout")), llm.calls[1].messages.last().content)
    }

    @Test
    fun `monolithic validation correction succeeds on exactly second attempt`() = runBlocking {
        val invalid = monolithicJson.replace("\"confidence\":0.9", "\"confidence\":85")
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(invalid))),
            Outcome.Return(Result.success(ChatResponse(monolithicJson)))
        ))

        val result = InferenceEngine(llm).execute("incident", InferenceMode.MONOLITHIC).getOrThrow()

        assertEquals(2, llm.calls.size)
        assertEquals(2, result.debugMetadata.totalModelCalls)
    }

    @Test
    fun `corrected normalization alone is passed to decision`() = runBlocking {
        val invalid = """{"observed_facts":"invalid","normalized_summary":"summary"}"""
        val corrected = """{"observed_facts":["explicit fact"],"normalized_summary":"summary"}"""
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(invalid))),
            Outcome.Return(Result.success(ChatResponse(corrected))),
            Outcome.Return(Result.success(ChatResponse(decisionJson))),
            Outcome.Return(Result.success(ChatResponse(presentationJson)))
        ))

        InferenceEngine(llm).execute("original", InferenceMode.MULTI_STAGE).getOrThrow()

        assertEquals(corrected, llm.calls[2].messages.last().content)
        assertFalse(llm.calls[2].messages.last().content.contains(invalid))
    }

    @Test
    fun `presentation validation retry succeeds and repeated validation failure falls back`() = runBlocking {
        val invalid = """{"title":"","message":"message","user_action":"retry"}"""
        val successClient = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(normalizationJson))),
            Outcome.Return(Result.success(ChatResponse(decisionJson))),
            Outcome.Return(Result.success(ChatResponse(invalid))),
            Outcome.Return(Result.success(ChatResponse(presentationJson)))
        ))
        val success = InferenceEngine(successClient).execute("incident", InferenceMode.MULTI_STAGE).getOrThrow()
        assertEquals(4, success.debugMetadata.totalModelCalls)
        assertEquals(StageStatus.OK, success.debugMetadata.stageMetadata.last().status)

        val failureClient = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse(normalizationJson))),
            Outcome.Return(Result.success(ChatResponse(decisionJson))),
            Outcome.Return(Result.success(ChatResponse(invalid))),
            Outcome.Return(Result.success(ChatResponse(invalid)))
        ))
        val fallback = InferenceEngine(failureClient).execute("incident", InferenceMode.MULTI_STAGE).getOrThrow()
        assertEquals(4, failureClient.calls.size)
        assertEquals(StageStatus.VALIDATION_ERROR, fallback.debugMetadata.stageMetadata.last().status)
        assertEquals(IncidentCategory.OPENAI_TIMEOUT, fallback.debugMetadata.decision?.category)
        assertFalse(fallback.debugMetadata.formatCompliant)
    }

    @Test
    fun `direct model exception is controlled and does not retry`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(Outcome.Throw(IllegalStateException())))
        val failure = InferenceEngine(llm).execute("incident", InferenceMode.MONOLITHIC).exceptionOrNull() as InferencePipelineException

        assertEquals(1, llm.calls.size)
        assertEquals(StageStatus.MODEL_ERROR, failure.debugMetadata.stageMetadata.single().status)
        assertEquals("IllegalStateException", failure.message)
    }

    @Test
    fun `cancellation during correction retry propagates without third call`() = runBlocking {
        val cancellation = CancellationException("cancel retry")
        val llm = FakeLlmClient(mutableListOf(
            Outcome.Return(Result.success(ChatResponse("""{"observed_facts":"invalid","normalized_summary":"summary"}"""))),
            Outcome.Throw(cancellation)
        ))
        try {
            InferenceEngine(llm).execute("incident", InferenceMode.MULTI_STAGE)
            fail("Expected CancellationException")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertEquals(2, llm.calls.size)
    }

    @Test
    fun `directly thrown cancellation is propagated`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        assertCancellation(cancellation, FakeLlmClient(mutableListOf(Outcome.Throw(cancellation))))
    }

    @Test
    fun `result wrapped cancellation is propagated without later calls`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        assertCancellation(cancellation, FakeLlmClient(mutableListOf(Outcome.Return(Result.failure(cancellation)))))
    }

    private suspend fun assertCancellation(expected: CancellationException, llm: FakeLlmClient) {
        try {
            InferenceEngine(llm).execute("incident", InferenceMode.MULTI_STAGE)
            fail("Expected CancellationException")
        } catch (actual: CancellationException) {
            assertSame(expected, actual)
        }
        assertEquals(1, llm.calls.size)
    }

    private fun tokenMetadata(prompt: Int, completion: Int) = AiResponseMetadata(
        modelDisplayName = "test",
        modelApiName = "test",
        responseTimeMs = 0,
        promptTokens = prompt,
        completionTokens = completion,
        totalTokens = prompt + completion,
        estimatedCostUsd = null
    )

    private data class Call(val messages: List<Message>, val model: String?, val options: LlmRequestOptions)
    private sealed interface Outcome {
        data class Return(val result: Result<ChatResponse>) : Outcome
        data class Throw(val error: Exception) : Outcome
    }

    private class FakeLlmClient(private val outcomes: MutableList<Outcome>) : LlmClient {
        val calls = mutableListOf<Call>()
        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> =
            error("Structured overload expected")

        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?, options: LlmRequestOptions): Result<ChatResponse> {
            calls += Call(messages, model, options)
            return when (val outcome = outcomes.removeAt(0)) {
                is Outcome.Return -> outcome.result
                is Outcome.Throw -> throw outcome.error
            }
        }
    }

    private companion object {
        const val normalizationJson = """{"observed_facts":["request exceeded allowed response time"],"normalized_summary":"timeout"}"""
        const val decisionJson = """{"category":"OPENAI_TIMEOUT","severity":"HIGH","action":"RETRY_REQUEST","confidence":0.9,"evidence_state":"SUPPORTED","supporting_evidence":["request exceeded allowed response time"],"contradicting_evidence":[]}"""
        val presentationJson = presentationJson(IncidentAction.RETRY_REQUEST)
        val monolithicJson = """{"normalized_summary":"timeout","category":"OPENAI_TIMEOUT","severity":"HIGH","action":"RETRY_REQUEST","confidence":0.9,"evidence_state":"SUPPORTED","supporting_evidence":["request exceeded allowed response time"],"contradicting_evidence":[],"title":"${IncidentAction.RETRY_REQUEST.userFacingText()}","message":"${IncidentAction.RETRY_REQUEST.userFacingText()}","user_action":"${IncidentAction.RETRY_REQUEST.userFacingText()}"}"""
        fun presentationJson(action: IncidentAction) =
            """{"title":"${action.userFacingText()}","message":"${action.userFacingText()}","user_action":"${action.userFacingText()}"}"""
        fun russian(vararg codePoints: Int): String = String(codePoints, 0, codePoints.size)
    }
}
