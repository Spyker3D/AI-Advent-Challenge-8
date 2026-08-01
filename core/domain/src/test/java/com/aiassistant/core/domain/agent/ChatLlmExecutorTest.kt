package com.aiassistant.core.domain.agent

import com.aiassistant.core.domain.entity.AiResponseMetadata
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.routing.ModelRouter
import com.aiassistant.core.domain.routing.RoutingDebugMetadata
import com.aiassistant.core.domain.routing.RoutingDiagnosticsLogger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChatLlmExecutorTest {
    @Test fun `eligible local routing off bypasses router and emits disabled metadata`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(Result.success(response("local:model"))))
        val logger = FakeLogger()
        val executor = ChatLlmExecutor(llm, ModelRouter(llm, logger), logger)
        val result = executor.execute(messages(), 100, "local:model", true, false).getOrThrow()
        assertEquals(1, llm.calls.size)
        assertFalse(result.routingMetadata!!.routingEnabled)
        assertEquals("local:model", result.routingMetadata!!.finalModel)
        assertEquals(1, logger.items.size)
    }

    @Test fun `routing on uses small model and returns parsed answer only`() = runBlocking {
        val raw = """{"answer":"Only the user answer is returned","confidence":0.95,"needs_escalation":false,"ambiguity":"LOW","sufficient_context":true,"reason":"simple"}"""
        val llm = FakeLlmClient(mutableListOf(Result.success(ChatResponse(raw))))
        val logger = FakeLogger()
        val result = ChatLlmExecutor(llm, ModelRouter(llm, logger), logger)
            .execute(messages(), 100, "manual:model", true, true).getOrThrow()
        assertEquals("Only the user answer is returned", result.response.message)
        assertTrue(result.routingMetadata!!.routingEnabled)
        assertEquals("llama3.2:3b", llm.calls.single().model)
    }

    @Test fun `ineligible provider bypasses router and has no routing metadata`() = runBlocking {
        val llm = FakeLlmClient(mutableListOf(Result.success(response("gpt-model"))))
        val logger = FakeLogger()
        val result = ChatLlmExecutor(llm, ModelRouter(llm, logger), logger)
            .execute(messages(), 100, "gpt-model", false, true).getOrThrow()
        assertNull(result.routingMetadata)
        assertEquals("gpt-model", llm.calls.single().model)
        assertTrue(logger.items.isEmpty())
    }

    private fun messages() = listOf(Message("u", "hello", MessageRole.USER))
    private fun response(model: String) = ChatResponse("direct answer", metadata = AiResponseMetadata(model, model, 12, null, null, null, null))
    private data class Call(val messages: List<Message>, val model: String?)
    private class FakeLlmClient(private val responses: MutableList<Result<ChatResponse>>) : LlmClient {
        val calls = mutableListOf<Call>()
        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> {
            calls += Call(messages, model); return responses.removeAt(0)
        }
    }
    private class FakeLogger : RoutingDiagnosticsLogger {
        val items = mutableListOf<RoutingDebugMetadata>()
        override fun log(metadata: RoutingDebugMetadata) { items += metadata }
    }
}
