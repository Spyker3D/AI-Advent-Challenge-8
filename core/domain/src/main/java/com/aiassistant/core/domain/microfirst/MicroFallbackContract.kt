package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.inference.IncidentCategory
import com.aiassistant.core.domain.inference.IncidentAction
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class FallbackResponse(
    val category: IncidentCategory,
    val confidence: Double,
    val title: String,
    val message: String,
    val userAction: String
)

internal sealed class FallbackParseResult {
    data class Success(val value: FallbackResponse) : FallbackParseResult()
    data class Failure(val validationFailure: Boolean, val error: String) : FallbackParseResult()
}

internal object MicroFallbackContract {
    const val PROMPT = """Classify only the current user request. Return exactly one JSON object matching the schema. Use Russian for title, message, and user_action. Do not put enum identifiers in user_action."""
    const val CORRECTION = """The previous JSON contained invalid values. Correct it and return only one JSON object matching the schema."""
    val SCHEMA: String = """{"type":"object","properties":{"category":{"type":"string","enum":[${IncidentCategory.entries.joinToString { "\"${it.name}\"" }}]},"confidence":{"type":"number","minimum":0,"maximum":1},"title":{"type":"string"},"message":{"type":"string"},"user_action":{"type":"string"}},"required":["category","confidence","title","message","user_action"],"additionalProperties":false}"""

    fun parse(raw: String): FallbackParseResult {
        val value = try {
            JsonParser.parseString(raw)
        } catch (_: Exception) {
            return FallbackParseResult.Failure(true, "Invalid JSON")
        }
        if (!value.isJsonObject) return FallbackParseResult.Failure(true, "Expected one JSON object")
        return try {
            val objectValue = value.asJsonObject
            require(objectValue.keySet() == FIELDS) { "Unexpected or missing fields" }
            val category = enumValueOf<IncidentCategory>(objectValue.string("category"))
            val confidence = objectValue.number("confidence")
            require(confidence in 0.0..1.0) { "confidence must be between 0 and 1" }
            val title = objectValue.nonBlank("title")
            val message = objectValue.nonBlank("message")
            val action = objectValue.nonBlank("user_action")
            require(listOf(title, message, action).all(::containsCyrillic)) { "presentation fields must contain Russian text" }
            val enumNames = IncidentCategory.entries.map { it.name } + IncidentAction.entries.map { it.name }
            require(enumNames.none { action.contains(it, ignoreCase = true) }) { "user_action must not contain enum identifiers" }
            FallbackParseResult.Success(FallbackResponse(category, confidence, title, message, action))
        } catch (e: Exception) {
            FallbackParseResult.Failure(true, e.message ?: "Invalid value")
        }
    }

    private fun JsonObject.string(key: String): String =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            ?: error("$key must be string")
    private fun JsonObject.nonBlank(key: String) = string(key).also { require(it.isNotBlank()) { "$key must not be blank" } }
    private fun JsonObject.number(key: String): Double =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble
            ?: error("$key must be number")
    private fun containsCyrillic(value: String) = value.any { it.code in 0x0400..0x04ff }
    private val FIELDS = setOf("category", "confidence", "title", "message", "user_action")
}
