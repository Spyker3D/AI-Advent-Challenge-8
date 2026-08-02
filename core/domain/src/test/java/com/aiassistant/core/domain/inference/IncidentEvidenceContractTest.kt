package com.aiassistant.core.domain.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IncidentEvidenceContractTest {
    @Test
    fun `specific category rejects explicit contradiction`() {
        assertThrows(IllegalArgumentException::class.java) {
            decision(
                category = IncidentCategory.OPENAI_TIMEOUT,
                state = EvidenceState.SUPPORTED,
                support = listOf("request exceeded allowed time"),
                contradictions = listOf("request completed within allowed time")
            ).validate()
        }
    }

    @Test
    fun `conflicting evidence requires ambiguous decision`() {
        decision(
            category = IncidentCategory.AMBIGUOUS,
            state = EvidenceState.CONFLICTING,
            support = listOf("one explicit signal"),
            contradictions = listOf("incompatible explicit signal")
        ).validate()
    }

    @Test
    fun `conflicting evidence requires both evidence sides`() {
        assertThrows(IllegalArgumentException::class.java) {
            decision(
                category = IncidentCategory.AMBIGUOUS,
                state = EvidenceState.CONFLICTING,
                contradictions = listOf("contradicting fact")
            ).validate()
        }
    }

    @Test
    fun `insufficient evidence requires ambiguous decision`() {
        decision(
            category = IncidentCategory.AMBIGUOUS,
            state = EvidenceState.INSUFFICIENT
        ).validate()
    }

    @Test
    fun `direct supporting evidence accepts every specific category and fixed action`() {
        IncidentCategory.entries.filterNot { it == IncidentCategory.AMBIGUOUS }.forEach { category ->
            val value = decision(
                category = category,
                state = EvidenceState.SUPPORTED,
                support = listOf("explicit supporting fact")
            )
            value.validate()
            assertEquals(category.requiredAction(), value.action)
        }
    }

    @Test
    fun `fact excluding an alternative does not conflict with directly supported category`() {
        val value = decision(
            category = IncidentCategory.OPENAI_RATE_LIMIT,
            state = EvidenceState.SUPPORTED,
            support = listOf("selected category has direct support"),
            contradictions = emptyList()
        )

        value.validate()

        assertEquals(EvidenceState.SUPPORTED, value.evidenceState)
    }

    @Test
    fun `conflicting state rejects a specific category`() {
        assertThrows(IllegalArgumentException::class.java) {
            decision(
                category = IncidentCategory.OPENAI_RATE_LIMIT,
                state = EvidenceState.CONFLICTING,
                support = listOf("direct support"),
                contradictions = listOf("directly incompatible fact")
            ).validate()
        }
    }

    @Test
    fun `classification contract is shared by decision and monolithic prompts`() {
        assertEquals(true, MultiStagePrompts.DECISION.contains(MultiStagePrompts.CLASSIFICATION_CONTRACT))
        assertEquals(true, MultiStagePrompts.MONOLITHIC.contains(MultiStagePrompts.CLASSIFICATION_CONTRACT))
    }

    private fun decision(
        category: IncidentCategory,
        state: EvidenceState,
        support: List<String> = emptyList(),
        contradictions: List<String> = emptyList()
    ) = IncidentDecision(
        category = category,
        severity = IncidentSeverity.MEDIUM,
        action = category.requiredAction(),
        confidence = 0.75,
        evidenceState = state,
        supportingEvidence = support,
        contradictingEvidence = contradictions
    )
}
