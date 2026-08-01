package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.inference.InferenceEngine
import com.aiassistant.core.domain.inference.InferenceMode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunInferenceUseCaseTest {
    @Test
    fun `delegates input and mode to inference engine`() = runBlocking {
        val client = RecordingClient()

        val result = RunInferenceUseCase(InferenceEngine(client))(
            "observed timeout",
            InferenceMode.MONOLITHIC
        ).getOrThrow()

        assertEquals("observed timeout", client.userInput)
        assertTrue(result.finalText.contains("Retry"))
        assertEquals(InferenceMode.MONOLITHIC, result.debugMetadata.mode)
    }

    @Test
    fun `returns inference failure`() = runBlocking {
        val expected = IllegalStateException("model failed")
        val result = RunInferenceUseCase(InferenceEngine(RecordingClient(Result.failure(expected))))(
            "incident", InferenceMode.MONOLITHIC
        )
        assertTrue(result.isFailure)
        assertEquals("model failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `propagates cancellation`() = runBlocking {
        val expected = CancellationException("cancelled")
        try {
            RunInferenceUseCase(InferenceEngine(RecordingClient(Result.failure(expected))))(
                "incident", InferenceMode.MONOLITHIC
            )
            fail("Expected CancellationException")
        } catch (actual: CancellationException) {
            assertSame(expected, actual)
        }
    }

    private class RecordingClient(
        private val response: Result<ChatResponse> = Result.success(ChatResponse(MONOLITHIC_JSON))
    ) : LlmClient {
        var userInput: String? = null

        override suspend fun sendChat(
            messages: List<Message>,
            maxTokens: Int?,
            model: String?
        ): Result<ChatResponse> = error("Structured overload expected")

        override suspend fun sendChat(
            messages: List<Message>,
            maxTokens: Int?,
            model: String?,
            options: LlmRequestOptions
        ): Result<ChatResponse> {
            userInput = messages.last().content
            return response
        }
    }

    private companion object {
        const val MONOLITHIC_JSON = """{"normalized_summary":"timeout","category":"OPENAI_TIMEOUT","severity":"HIGH","action":"RETRY_REQUEST","confidence":0.9,"evidence_state":"SUPPORTED","supporting_evidence":["request exceeded allowed response time"],"contradicting_evidence":[],"title":"Retry","message":"Timed out","user_action":"Try again"}"""
    }
}
