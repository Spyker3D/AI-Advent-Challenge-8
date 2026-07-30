package com.aiassistant.core.domain.routing

import org.junit.Assert.*
import org.junit.Test

class SmallModelDecisionParserTest {
    private val valid = """{"answer":"A sufficiently long answer","confidence":0.9,"needs_escalation":false,"reason":"simple"}"""

    @Test fun `parses ordinary JSON`() { assertEquals(0.9, SmallModelDecisionParser.parse(valid).getOrThrow().confidence, 0.0) }
    @Test fun `parses fenced JSON`() { assertTrue(SmallModelDecisionParser.parse("```json\n$valid\n```").isSuccess) }
    @Test fun `rejects string confidence`() { assertTrue(SmallModelDecisionParser.parse(valid.replace("0.9", "\"0.9\"")).isFailure) }
    @Test fun `rejects confidence outside range`() { assertTrue(SmallModelDecisionParser.parse(valid.replace("0.9", "1.1")).isFailure) }
    @Test fun `rejects missing field`() { assertTrue(SmallModelDecisionParser.parse("""{"answer":"answer","confidence":0.9,"needs_escalation":false}""").isFailure) }
    @Test fun `allows unknown fields`() { assertTrue(SmallModelDecisionParser.parse(valid.dropLast(1) + ",\"extra\":true}").isSuccess) }
    @Test fun `rejects blank answer`() { assertTrue(SmallModelDecisionParser.parse(valid.replace("A sufficiently long answer", " ")).isFailure) }
}
