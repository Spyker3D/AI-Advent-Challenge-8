package com.aiassistant.core.domain.routing

import com.google.gson.JsonObject
import com.google.gson.JsonParser

class SmallModelDecisionParseException(val category: RoutingParseFailure, message: String) : IllegalArgumentException(message)

object SmallModelDecisionParser {
    fun parse(raw: String): Result<SmallModelDecision> = runCatching {
        val normalized = extractJson(raw)
        if (Regex("""\{\s*[A-Za-z_][A-Za-z0-9_]*\s*:""").containsMatchIn(normalized)) {
            fail(RoutingParseFailure.MISSING_QUOTES, "JSON object keys must be quoted")
        }
        val value = runCatching { JsonParser.parseString(normalized) }.getOrElse {
            throw SmallModelDecisionParseException(RoutingParseFailure.OTHER, "Invalid JSON")
        }
        if (!value.isJsonObject) fail(RoutingParseFailure.TYPE_MISMATCH, "Response must be a JSON object")
        val json = value.asJsonObject
        val answer = required(json, "answer")
        val confidence = required(json, "confidence")
        val escalation = required(json, "needs_escalation")
        val ambiguity = required(json, "ambiguity")
        val sufficientContext = required(json, "sufficient_context")
        val reason = required(json, "reason")

        if (!answer.isJsonPrimitive || !answer.asJsonPrimitive.isString) fail(RoutingParseFailure.TYPE_MISMATCH, "answer must be a string")
        if (!confidence.isJsonPrimitive) fail(RoutingParseFailure.TYPE_MISMATCH, "confidence must be a number")
        if (!escalation.isJsonPrimitive || !escalation.asJsonPrimitive.isBoolean) fail(RoutingParseFailure.TYPE_MISMATCH, "needs_escalation must be boolean")
        if (!ambiguity.isJsonPrimitive || !ambiguity.asJsonPrimitive.isString) fail(RoutingParseFailure.TYPE_MISMATCH, "ambiguity must be a string")
        if (!sufficientContext.isJsonPrimitive || !sufficientContext.asJsonPrimitive.isBoolean) fail(RoutingParseFailure.TYPE_MISMATCH, "sufficient_context must be boolean")
        if (!reason.isJsonPrimitive || !reason.asJsonPrimitive.isString) fail(RoutingParseFailure.TYPE_MISMATCH, "reason must be a string")

        val answerText = answer.asString.trim()
        val confidenceValue = confidence.asJsonPrimitive.let { primitive ->
            when {
                primitive.isBoolean -> fail(RoutingParseFailure.TYPE_MISMATCH, "confidence must be a number")
                primitive.isNumber -> primitive.asDouble
                primitive.isString -> primitive.asString.toDoubleOrNull()
                    ?: fail(RoutingParseFailure.TYPE_MISMATCH, "confidence must be numeric")
                else -> fail(RoutingParseFailure.TYPE_MISMATCH, "confidence must be a number")
            }
        }
        val ambiguityValue = runCatching { Ambiguity.valueOf(ambiguity.asString) }.getOrElse {
            fail(RoutingParseFailure.TYPE_MISMATCH, "ambiguity must be LOW, MEDIUM, or HIGH")
        }
        val reasonText = reason.asString.trim()
        if (answerText.isEmpty()) fail(RoutingParseFailure.TYPE_MISMATCH, "answer must not be blank")
        if (!confidenceValue.isFinite() || confidenceValue !in 0.0..1.0) fail(RoutingParseFailure.TYPE_MISMATCH, "confidence must be in 0.0..1.0")
        if (reasonText.isEmpty()) fail(RoutingParseFailure.EMPTY_REASON, "reason must not be blank")

        SmallModelDecision(
            answer = answerText,
            confidence = confidenceValue,
            needsEscalation = escalation.asBoolean,
            ambiguity = ambiguityValue,
            sufficientContext = sufficientContext.asBoolean,
            reason = reasonText
        )
    }

    fun failureCategory(error: Throwable): RoutingParseFailure =
        (error as? SmallModelDecisionParseException)?.category ?: RoutingParseFailure.OTHER

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val fence = Regex("""^```(?:json)?\s*([\s\S]*?)\s*```$""", RegexOption.IGNORE_CASE)
        val unfenced = fence.matchEntire(trimmed)?.groupValues?.get(1)?.trim() ?: trimmed
        val start = unfenced.indexOf('{')
        val end = unfenced.lastIndexOf('}')
        return if (start >= 0 && end >= start) unfenced.substring(start, end + 1) else unfenced
    }

    private fun required(json: JsonObject, name: String) =
        json.get(name) ?: fail(RoutingParseFailure.OTHER, "Missing field: $name")

    private fun fail(category: RoutingParseFailure, message: String): Nothing =
        throw SmallModelDecisionParseException(category, message)
}
