package com.aiassistant.core.domain.inference

import com.google.gson.JsonObject
import com.google.gson.JsonParser

sealed class ParseResult<out T> { data class Success<T>(val value: T) : ParseResult<T>(); data class Failure(val status: StageStatus, val error: String) : ParseResult<Nothing>() }

object InferenceParsers {
    fun normalization(raw: String): ParseResult<NormalizedIncident> = parse(raw, setOf("observed_facts","normalized_summary")) { o ->
        NormalizedIncident(o.stringList("observed_facts"), o.nonBlank("normalized_summary"))
    }
    fun decision(raw: String): ParseResult<IncidentDecision> = parse(raw, DECISION_FIELDS) { o ->
        val result = o.decision()
        result.validate()
        result
    }
    fun presentation(raw: String): ParseResult<UserFacingIncidentResult> = parse(raw, setOf("title","message","user_action")) { o -> UserFacingIncidentResult(o.nonBlank("title"), o.nonBlank("message"), o.nonBlank("user_action")) }
    fun monolithic(raw: String): ParseResult<Triple<String, IncidentDecision, UserFacingIncidentResult>> = parse(raw, DECISION_FIELDS + setOf("normalized_summary","title","message","user_action")) { o ->
        val decision = o.decision()
        decision.validate()
        Triple(o.nonBlank("normalized_summary"), decision, UserFacingIncidentResult(o.nonBlank("title"), o.nonBlank("message"), o.nonBlank("user_action")))
    }
    fun normalizedJson(value: NormalizedIncident) = com.google.gson.Gson().toJson(mapOf("observed_facts" to value.observedFacts,"normalized_summary" to value.normalizedSummary))
    fun decisionJson(value: IncidentDecision) = com.google.gson.Gson().toJson(mapOf("category" to value.category.name,"severity" to value.severity.name,"action" to value.action.name,"confidence" to value.confidence,"evidence_state" to value.evidenceState.name,"supporting_evidence" to value.supportingEvidence,"contradicting_evidence" to value.contradictingEvidence))

    private fun <T> parse(raw: String, fields: Set<String>, build: (JsonObject) -> T): ParseResult<T> {
        val objectValue = try { JsonParser.parseString(raw).takeIf { it.isJsonObject }?.asJsonObject ?: return ParseResult.Failure(StageStatus.FORMAT_ERROR, "Expected one JSON object") } catch (e: Exception) { return ParseResult.Failure(StageStatus.FORMAT_ERROR, "Invalid JSON") }
        return try { require(objectValue.keySet() == fields) { "Unexpected or missing fields" }; ParseResult.Success(build(objectValue)) } catch (e: Exception) { ParseResult.Failure(StageStatus.VALIDATION_ERROR, e.message ?: "Invalid value") }
    }
    private fun JsonObject.string(k:String)=get(k).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: error("$k must be string")
    private fun JsonObject.nonBlank(k:String)=string(k).also { require(it.isNotBlank()) { "$k must not be blank" } }
    private fun JsonObject.number(k:String)=get(k).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble ?: error("$k must be number")
    private fun JsonObject.stringList(k:String)=get(k).takeIf { it.isJsonArray }?.asJsonArray?.map { value -> value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.also { require(it.isNotBlank()) { "$k values must not be blank" } } ?: error("$k values must be strings") } ?: error("$k must be array")
    private fun JsonObject.decision() = IncidentDecision(enumValueOf(string("category")),enumValueOf(string("severity")),enumValueOf(string("action")),number("confidence"),enumValueOf(string("evidence_state")),stringList("supporting_evidence"),stringList("contradicting_evidence"))
    private val DECISION_FIELDS = setOf("category","severity","action","confidence","evidence_state","supporting_evidence","contradicting_evidence")
}
