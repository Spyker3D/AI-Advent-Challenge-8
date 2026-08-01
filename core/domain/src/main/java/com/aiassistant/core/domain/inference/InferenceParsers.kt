package com.aiassistant.core.domain.inference

import com.google.gson.JsonObject
import com.google.gson.JsonParser

sealed class ParseResult<out T> { data class Success<T>(val value: T) : ParseResult<T>(); data class Failure(val status: StageStatus, val error: String) : ParseResult<Nothing>() }

object InferenceParsers {
    fun normalization(raw: String): ParseResult<NormalizedIncident> = parse(raw, setOf("network_available","http_status","timeout_observed","empty_response","local_history_problem","multiple_signals","normalized_summary")) { o ->
        NormalizedIncident(o.nullableBoolean("network_available"), o.nullableInt("http_status"), o.boolean("timeout_observed"), o.boolean("empty_response"), o.boolean("local_history_problem"), o.boolean("multiple_signals"), o.nonBlank("normalized_summary"))
    }
    fun decision(raw: String): ParseResult<IncidentDecision> = parse(raw, setOf("category","severity","action","confidence")) { o ->
        val result = IncidentDecision(enumValueOf(o.string("category")), enumValueOf(o.string("severity")), enumValueOf(o.string("action")), o.number("confidence"))
        require(result.confidence in 0.0..1.0) { "confidence must be between 0 and 1" }
        require(result.category != IncidentCategory.AMBIGUOUS || (result.action == IncidentAction.REQUEST_MORE_INFORMATION && result.confidence <= 0.8)) { "invalid ambiguous decision" }
        result
    }
    fun presentation(raw: String): ParseResult<UserFacingIncidentResult> = parse(raw, setOf("title","message","user_action")) { o -> UserFacingIncidentResult(o.nonBlank("title"), o.nonBlank("message"), o.nonBlank("user_action")) }
    fun monolithic(raw: String): ParseResult<Triple<String, IncidentDecision, UserFacingIncidentResult>> = parse(raw, setOf("normalized_summary","category","severity","action","confidence","title","message","user_action")) { o ->
        val decision = IncidentDecision(enumValueOf(o.string("category")), enumValueOf(o.string("severity")), enumValueOf(o.string("action")), o.number("confidence"))
        require(decision.confidence in 0.0..1.0 && (decision.category != IncidentCategory.AMBIGUOUS || decision.action == IncidentAction.REQUEST_MORE_INFORMATION))
        Triple(o.nonBlank("normalized_summary"), decision, UserFacingIncidentResult(o.nonBlank("title"), o.nonBlank("message"), o.nonBlank("user_action")))
    }
    fun normalizedJson(value: NormalizedIncident) = com.google.gson.Gson().toJson(mapOf("network_available" to value.networkAvailable,"http_status" to value.httpStatus,"timeout_observed" to value.timeoutObserved,"empty_response" to value.emptyResponse,"local_history_problem" to value.localHistoryProblem,"multiple_signals" to value.multipleSignals,"normalized_summary" to value.normalizedSummary))
    fun decisionJson(value: IncidentDecision) = com.google.gson.Gson().toJson(mapOf("category" to value.category.name,"severity" to value.severity.name,"action" to value.action.name,"confidence" to value.confidence))

    private fun <T> parse(raw: String, fields: Set<String>, build: (JsonObject) -> T): ParseResult<T> {
        val objectValue = try { JsonParser.parseString(raw).takeIf { it.isJsonObject }?.asJsonObject ?: return ParseResult.Failure(StageStatus.FORMAT_ERROR, "Expected one JSON object") } catch (e: Exception) { return ParseResult.Failure(StageStatus.FORMAT_ERROR, "Invalid JSON") }
        return try { require(objectValue.keySet() == fields) { "Unexpected or missing fields" }; ParseResult.Success(build(objectValue)) } catch (e: Exception) { ParseResult.Failure(StageStatus.VALIDATION_ERROR, e.message ?: "Invalid value") }
    }
    private fun JsonObject.string(k:String)=get(k).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: error("$k must be string")
    private fun JsonObject.nonBlank(k:String)=string(k).also { require(it.isNotBlank()) { "$k must not be blank" } }
    private fun JsonObject.boolean(k:String)=get(k).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean ?: error("$k must be boolean")
    private fun JsonObject.number(k:String)=get(k).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble ?: error("$k must be number")
    private fun JsonObject.nullableBoolean(k:String)=get(k).let { if (it.isJsonNull) null else it.takeIf { v -> v.isJsonPrimitive && v.asJsonPrimitive.isBoolean }?.asBoolean ?: error("$k must be boolean or null") }
    private fun JsonObject.nullableInt(k:String)=get(k).let { if (it.isJsonNull) null else it.takeIf { v -> v.isJsonPrimitive && v.asJsonPrimitive.isNumber && v.asDouble % 1.0 == 0.0 }?.asInt ?: error("$k must be integer or null") }
}
