package com.aiassistant.core.domain.routing

import org.junit.Assert.*
import org.junit.Test

class SmallModelDecisionParserTest {
    private val valid = """{"answer":"A sufficiently long answer","confidence":0.9,"needs_escalation":false,"ambiguity":"LOW","sufficient_context":true,"reason":"simple"}"""

    @Test fun `parses ordinary JSON`() { assertEquals(0.9, SmallModelDecisionParser.parse(valid).getOrThrow().confidence, 0.0) }
    @Test fun `parses fenced JSON`() { assertTrue(SmallModelDecisionParser.parse("```json\n$valid\n```").isSuccess) }
    @Test fun `accepts numeric string confidence`() { assertTrue(SmallModelDecisionParser.parse(valid.replace("0.9", "\"0.9\"")).isSuccess) }
    @Test fun `rejects confidence outside range`() { assertTrue(SmallModelDecisionParser.parse(valid.replace("0.9", "1.1")).isFailure) }
    @Test fun `rejects missing field`() { assertTrue(SmallModelDecisionParser.parse("""{"answer":"answer","confidence":0.9,"needs_escalation":false,"ambiguity":"LOW","sufficient_context":true}""").isFailure) }
    @Test fun `allows unknown fields`() { assertTrue(SmallModelDecisionParser.parse(valid.dropLast(1) + ",\"extra\":true}").isSuccess) }
    @Test fun `rejects blank reason with category`() {
        val error = SmallModelDecisionParser.parse(valid.replace("simple", " ")).exceptionOrNull()!!
        assertEquals(RoutingParseFailure.EMPTY_REASON, SmallModelDecisionParser.failureCategory(error))
    }
    @Test fun `rejects unquoted keys`() { assertTrue(SmallModelDecisionParser.parse("{answer: \"x\", confidence: 1.0}").isFailure) }
    @Test fun `rejects blank answer`() { assertTrue(SmallModelDecisionParser.parse(valid.replace("A sufficiently long answer", " ")).isFailure) }
    @Test fun `parses every ambiguity value`() {
        Ambiguity.values().forEach { ambiguity ->
            assertEquals(ambiguity, SmallModelDecisionParser.parse(valid.replace("LOW", ambiguity.name)).getOrThrow().ambiguity)
        }
    }
    @Test fun `rejects missing ambiguity`() {
        assertTrue(SmallModelDecisionParser.parse(valid.replace("\"ambiguity\":\"LOW\",", "")).isFailure)
    }
    @Test fun `rejects unknown ambiguity`() {
        val error = SmallModelDecisionParser.parse(valid.replace("LOW", "UNKNOWN")).exceptionOrNull()!!
        assertEquals(RoutingParseFailure.TYPE_MISMATCH, SmallModelDecisionParser.failureCategory(error))
    }
    @Test fun `rejects non boolean sufficient context`() {
        val error = SmallModelDecisionParser.parse(valid.replace("\"sufficient_context\":true", "\"sufficient_context\":\"true\"")).exceptionOrNull()!!
        assertEquals(RoutingParseFailure.TYPE_MISMATCH, SmallModelDecisionParser.failureCategory(error))
    }}
