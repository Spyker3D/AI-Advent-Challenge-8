package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.inference.IncidentCategory
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

internal data class FallbackResponse(
    val category: IncidentCategory,
    val confidence: Double,
    val reason: String
)

internal sealed class FallbackParseResult {
    data class Success(val value: FallbackResponse) : FallbackParseResult()
    data class Failure(val validationFailure: Boolean, val error: String) : FallbackParseResult()
}

internal object MicroFallbackContract {
    const val PROMPT = """Use a cause-first algorithm: identify the reported failure cause, distinguish it from symptoms and advice, then choose exactly one category. NETWORK_UNAVAILABLE means absent network connectivity. OPENAI_RATE_LIMIT means excessive request frequency, volume, quota, throttling, or HTTP 429. OPENAI_TIMEOUT means elapsed duration, expired deadline, or no timely response. EMPTY_AI_RESPONSE means a completed response with no usable content. LOCAL_HISTORY_UNAVAILABLE means locally stored chat history cannot be read or restored. AMBIGUOUS means evidence is insufficient or supports multiple causes. A recommendation to retry later does not determine the category and, without a stated cause, is AMBIGUOUS. Return exactly one JSON object containing only category, confidence, and reason. reason must be a short Russian explanation of the cause. Do not add title, message, user_action, or other presentation fields."""
    const val CORRECTION = """The previous JSON was structurally invalid. Correct it and return only one JSON object matching the schema."""
    private val categoryEnum = IncidentCategory.entries.joinToString { JsonPrimitive(it.name).toString() }
    val SCHEMA: String = """{"type":"object","properties":{"category":{"type":"string","enum":[$categoryEnum]},"confidence":{"type":"number","minimum":0,"maximum":1},"reason":{"type":"string","minLength":1,"maxLength":160}},"required":["category","confidence","reason"],"additionalProperties":false}"""

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
            val reason = objectValue.nonBlank("reason")
            require(reason.length <= MAX_REASON_LENGTH) { "reason must be short" }
            require(containsCyrillic(reason)) { "reason must contain Russian text" }
            FallbackParseResult.Success(FallbackResponse(category, confidence, reason))
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
    private val FIELDS = setOf("category", "confidence", "reason")
    private const val MAX_REASON_LENGTH = 160
}
