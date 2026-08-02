package com.aiassistant.core.domain.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceParsersTest {
    @Test
    fun `decision accepts every consistent category and action`() {
        IncidentCategory.entries.forEach { category ->
            val confidence = 0.8
            assertTrue(InferenceParsers.decision(decisionJson(category, category.requiredAction(), confidence)) is ParseResult.Success)
        }
    }

    @Test
    fun `decision rejects inconsistent category and action`() {
        IncidentCategory.entries.forEach { category ->
            val wrongAction = IncidentAction.entries.first { it != category.requiredAction() }
            val result = InferenceParsers.decision(decisionJson(category, wrongAction, 0.5))
            assertValidationFailure(result)
        }
    }

    @Test
    fun `confidence must be a fraction encoded as a number`() {
        assertValidationFailure(InferenceParsers.decision(decisionJson(IncidentCategory.OPENAI_TIMEOUT, IncidentAction.RETRY_REQUEST, 85.0)))
        assertValidationFailure(InferenceParsers.decision(decisionJson(IncidentCategory.OPENAI_TIMEOUT, IncidentAction.RETRY_REQUEST, "\"85%\"")))
        assertValidationFailure(InferenceParsers.decision(decisionJson(IncidentCategory.OPENAI_TIMEOUT, IncidentAction.RETRY_REQUEST, "\"0.85\"")))
        assertTrue(InferenceParsers.decision(decisionJson(IncidentCategory.OPENAI_TIMEOUT, IncidentAction.RETRY_REQUEST, 0.85)) is ParseResult.Success)
    }

    @Test
    fun `decision accepts supported and rejects legacy sufficient evidence state`() {
        assertTrue(InferenceParsers.decision(decisionJson(IncidentCategory.OPENAI_RATE_LIMIT, IncidentAction.RETRY_WITH_BACKOFF, 0.8)) is ParseResult.Success)
        val legacy = decisionJson(IncidentCategory.OPENAI_RATE_LIMIT, IncidentAction.RETRY_WITH_BACKOFF, 0.8)
            .replace("\"SUPPORTED\"", "\"SUFFICIENT\"")
        assertValidationFailure(InferenceParsers.decision(legacy))
    }

    @Test
    fun `parsers reject unexpected fields invalid types and blank presentation`() {
        assertEquals(StageStatus.FORMAT_ERROR, (InferenceParsers.decision("not json") as ParseResult.Failure).status)
        assertValidationFailure(InferenceParsers.decision(decisionJson(IncidentCategory.OPENAI_TIMEOUT, IncidentAction.RETRY_REQUEST, "\"high\"")))
        assertValidationFailure(InferenceParsers.presentation("""{"title":"","message":"message","user_action":"retry"}""", IncidentAction.RETRY_REQUEST))
        assertValidationFailure(InferenceParsers.normalization("""{"observed_facts":["fact"],"normalized_summary":"ok","extra":1}"""))
    }

    @Test
    fun `presentation rejects enum action and accepts matching Russian action`() {
        val action = IncidentAction.RETRY_WITH_BACKOFF
        assertValidationFailure(
            InferenceParsers.presentation(presentationJson(action.name), action)
        )
        assertTrue(
            InferenceParsers.presentation(presentationJson(action.userFacingText()), action) is ParseResult.Success
        )
        assertTrue(IncidentAction.entries.none { it.name == action.userFacingText() })
    }

    @Test
    fun `presentation rejects English fields and a phrase for a different action`() {
        assertValidationFailure(
            InferenceParsers.presentation(
                """{"title":"Rate limit","message":"Try again later","user_action":"${IncidentAction.RETRY_WITH_BACKOFF.userFacingText()}"}""",
                IncidentAction.RETRY_WITH_BACKOFF
            )
        )
        assertValidationFailure(
            InferenceParsers.presentation(
                presentationJson(IncidentAction.CHECK_NETWORK.userFacingText()),
                IncidentAction.RETRY_WITH_BACKOFF
            )
        )
    }

    @Test
    fun `monolithic parser enforces the shared presentation contract`() {
        val action = IncidentAction.RETRY_WITH_BACKOFF
        val valid = monolithicJson(IncidentCategory.OPENAI_RATE_LIMIT, action, 0.85)
        assertTrue(InferenceParsers.monolithic(valid) is ParseResult.Success)
        assertValidationFailure(
            InferenceParsers.monolithic(valid.replace(action.userFacingText(), action.name))
        )
        assertValidationFailure(
            InferenceParsers.monolithic(valid.replace(action.userFacingText(), "Try again later"))
        )
        assertValidationFailure(
            InferenceParsers.monolithic(
                valid.replace(action.userFacingText(), IncidentAction.CHECK_NETWORK.userFacingText())
            )
        )
    }

    private fun decisionJson(category: IncidentCategory, action: IncidentAction, confidence: Any): String {
        val ambiguous = category == IncidentCategory.AMBIGUOUS
        val state = if (ambiguous) "INSUFFICIENT" else "SUPPORTED"
        val support = if (ambiguous) "[]" else "[\"explicit fact\"]"
        return """{"category":"${category.name}","severity":"HIGH","action":"${action.name}","confidence":$confidence,"evidence_state":"$state","supporting_evidence":$support,"contradicting_evidence":[]}"""
    }

    private fun monolithicJson(category: IncidentCategory, action: IncidentAction, confidence: Double) =
        """{"normalized_summary":"summary","category":"${category.name}","severity":"HIGH","action":"${action.name}","confidence":$confidence,"evidence_state":"SUPPORTED","supporting_evidence":["fact"],"contradicting_evidence":[],"title":"${action.userFacingText()}","message":"${action.userFacingText()}","user_action":"${action.userFacingText()}"}"""

    private fun presentationJson(userAction: String) =
        """{"title":"${IncidentAction.RETRY_WITH_BACKOFF.userFacingText()}","message":"${IncidentAction.RETRY_WITH_BACKOFF.userFacingText()}","user_action":"$userAction"}"""

    private fun assertValidationFailure(result: ParseResult<*>) {
        assertTrue(result is ParseResult.Failure)
        assertEquals(StageStatus.VALIDATION_ERROR, (result as ParseResult.Failure).status)
    }
}
