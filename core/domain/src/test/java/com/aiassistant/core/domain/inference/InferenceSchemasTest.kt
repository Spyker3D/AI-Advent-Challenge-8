package com.aiassistant.core.domain.inference

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class InferenceSchemasTest {
    @Test
    fun `decision schema contains every supported enum value`() {
        val properties = JsonParser.parseString(InferenceSchemas.DECISION)
            .asJsonObject.getAsJsonObject("properties")

        assertEquals(IncidentCategory.entries.map { it.name }, properties.enumNames("category"))
        assertEquals(IncidentSeverity.entries.map { it.name }, properties.enumNames("severity"))
        assertEquals(IncidentAction.entries.map { it.name }, properties.enumNames("action"))
        assertEquals(EvidenceState.entries.map { it.name }, properties.enumNames("evidence_state"))
        assertEquals(false, properties.enumNames("evidence_state").contains("SUFFICIENT"))
    }

    @Test
    fun `all schemas reject additional properties and declare required fields`() {
        listOf(
            InferenceSchemas.MONOLITHIC to 11,
            InferenceSchemas.NORMALIZATION to 2,
            InferenceSchemas.DECISION to 7,
            InferenceSchemas.PRESENTATION to 3
        ).forEach { (schema, requiredCount) ->
            val root = JsonParser.parseString(schema).asJsonObject
            assertEquals(false, root.get("additionalProperties").asBoolean)
            assertEquals(requiredCount, root.getAsJsonArray("required").size())
        }
    }

    @Test
    fun `presentation schemas allow only mapped user actions`() {
        val expected = IncidentAction.entries.map { it.userFacingText() }
        listOf(InferenceSchemas.MONOLITHIC, InferenceSchemas.PRESENTATION).forEach { schema ->
            val properties = JsonParser.parseString(schema).asJsonObject.getAsJsonObject("properties")
            assertEquals(expected, properties.enumNames("user_action"))
        }
    }

    private fun com.google.gson.JsonObject.enumNames(name: String): List<String> =
        getAsJsonObject(name).getAsJsonArray("enum").map { it.asString }
}
