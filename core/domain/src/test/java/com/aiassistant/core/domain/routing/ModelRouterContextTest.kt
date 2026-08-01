package com.aiassistant.core.domain.routing

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ModelRouterContextTest {
    @Test fun `fallback creates separate tier prompts with identical dialogue context`() = runBlocking {
        val raw = """{"answer":"uncertain preliminary answer","confidence":0.5,"needs_escalation":false,"ambiguity":"LOW","sufficient_context":true,"reason":"uncertain"}"""
        val llm = CapturingClient(mutableListOf(Result.success(ChatResponse(raw)), Result.success(ChatResponse("final expert answer"))))
        val prepared = listOf(
            Message("s", "application system context", MessageRole.SYSTEM),
            Message("u1", "earlier question", MessageRole.USER),
            Message("a1", "earlier answer", MessageRole.ASSISTANT),
            Message("u2", "current question", MessageRole.USER)
        )
        val result = ModelRouter(llm).route(prepared).getOrThrow()
        assertEquals("final expert answer", result.answer)
        assertEquals(2, llm.calls.size)
        val small = llm.calls[0].messages
        val large = llm.calls[1].messages
        assertTrue(small.first().content.contains(RoutingPrompts.SMALL_MODEL))
        assertFalse(small.first().content.contains(RoutingPrompts.LARGE_MODEL))
        assertTrue(large.first().content.contains(RoutingPrompts.LARGE_MODEL))
        assertFalse(large.first().content.contains(RoutingPrompts.SMALL_MODEL))
        assertEquals(small.drop(1), large.drop(1))
        assertEquals("current question", large.last().content)
        assertFalse(large.any { it.content.contains(raw) })
    }

    @Test fun `small prompt declares all routing fields and large prompt does not request confidence JSON`() {
        listOf("answer", "confidence", "needs_escalation", "ambiguity", "sufficient_context", "reason").forEach { assertTrue(RoutingPrompts.SMALL_MODEL.contains(it)) }
        assertFalse(RoutingPrompts.LARGE_MODEL.contains("Return JSON", ignoreCase = true))
        assertFalse(RoutingPrompts.LARGE_MODEL.contains("needs_escalation"))
    }

    private data class Call(val messages: List<Message>, val model: String?)
    private class CapturingClient(private val responses: MutableList<Result<ChatResponse>>) : LlmClient {
        val calls = mutableListOf<Call>()
        override suspend fun sendChat(messages: List<Message>, maxTokens: Int?, model: String?): Result<ChatResponse> {
            calls += Call(messages, model); return responses.removeAt(0)
        }
    }
}
