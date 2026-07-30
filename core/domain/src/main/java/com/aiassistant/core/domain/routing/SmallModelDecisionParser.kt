package com.aiassistant.core.domain.routing

import com.google.gson.JsonObject
import com.google.gson.JsonParser

object SmallModelDecisionParser {
    fun parse(raw: String): Result<SmallModelDecision> = runCatching {
        val value = JsonParser.parseString(extractJson(raw))
        require(value.isJsonObject) { "Response must be a JSON object" }
        val json = value.asJsonObject
        val answer = required(json, "answer")
        val confidence = required(json, "confidence")
        val escalation = required(json, "needs_escalation")
        val reason = required(json, "reason")
        require(answer.isJsonPrimitive && answer.asJsonPrimitive.isString) { "answer must be a string" }
        require(confidence.isJsonPrimitive && confidence.asJsonPrimitive.isNumber) { "confidence must be a number" }
        require(escalation.isJsonPrimitive && escalation.asJsonPrimitive.isBoolean) { "needs_escalation must be boolean" }
        require(reason.isJsonPrimitive && reason.asJsonPrimitive.isString) { "reason must be a string" }
        val answerText = answer.asString.trim()
        val confidenceValue = confidence.asDouble
        val reasonText = reason.asString.trim()
        require(answerText.isNotEmpty()) { "answer must not be blank" }
        require(confidenceValue.isFinite() && confidenceValue in 0.0..1.0) { "confidence must be in 0.0..1.0" }
        require(reasonText.isNotEmpty()) { "reason must not be blank" }
        SmallModelDecision(answerText, confidenceValue, escalation.asBoolean, reasonText)
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val fence = Regex("""^```(?:json)?\s*([\s\S]*?)\s*```$""", RegexOption.IGNORE_CASE)
        return fence.matchEntire(trimmed)?.groupValues?.get(1)?.trim() ?: trimmed
    }

    private fun required(json: JsonObject, name: String) = requireNotNull(json.get(name)) { "Missing field: $name" }
}
