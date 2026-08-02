package com.aiassistant.day10

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.sqrt

class MicroFirstRunner(
    private val gateway: OllamaGateway,
    private val minScore: Double = MIN_SCORE,
    private val minMargin: Double = MIN_MARGIN,
    private val gson: Gson = defaultGson()
) {
    fun loadPrototypes(path: Path): Map<String, List<String>> {
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        val values: Map<String, List<String>> = Files.newBufferedReader(path).use { gson.fromJson(it, type) }
        require(values.keys == LABELS.toSet() && values.values.all { it.distinct().size >= 8 })
        return values
    }

    fun loadCases(path: Path): List<TestCase> = Files.readAllLines(path)
        .filter { it.isNotBlank() }.map { gson.fromJson(it, TestCase::class.java) }

    fun run(prototypes: Map<String, List<String>>, cases: List<TestCase>): RunSummary {
        val flat = LABELS.flatMap { label -> prototypes.getValue(label).map { label to it } }
        var references: Map<String, List<List<Double>>>? = null
        var setupError: String? = null
        try {
            val vectors = validateBatch(gateway.embed(flat.map { it.second }), flat.size)
            references = flat.zip(vectors).groupBy({ it.first.first }, { it.second })
        } catch (error: Exception) {
            setupError = error.message ?: error.javaClass.simpleName
        }
        val rows = cases.map { evaluate(it, references, setupError) }
        return RunSummary(
            results = rows,
            totalRequests = rows.size,
            microHandled = rows.count { it.actualRoute == "MICRO" },
            fallbackHandled = rows.count { it.actualRoute == "FALLBACK" },
            largeModelCalls = rows.sumOf { it.largeLlmCalls },
            averageLatencyMs = rows.map { it.totalLatencyMs }.averageOrZero()
        )
    }

    fun decide(vector: List<Double>, references: Map<String, List<List<Double>>>): MicroDecision {
        val normalized = normalize(vector)
        val ranked = references.mapValues { (_, prototypes) -> prototypes.maxOf { cosine(normalized, it) } }
            .entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
        require(ranked.size >= 2) { "At least two labels are required" }
        val top = ranked[0]
        val second = ranked[1].value
        val margin = top.value - second
        val reason = when {
            top.value < minScore -> "LOW_SCORE"
            margin < minMargin -> "LOW_MARGIN"
            else -> null
        }
        return MicroDecision(top.key, top.value, second, margin, if (reason == null) "OK" else "UNSURE", reason)
    }

    fun parseFallback(raw: String): FallbackResponse {
        val value = try { JsonParser.parseString(raw) } catch (error: Exception) { throw IllegalArgumentException("Invalid JSON", error) }
        require(value.isJsonObject) { "Expected one object" }
        val objectValue = value.asJsonObject
        require(objectValue.keySet() == setOf("category", "confidence", "reason")) { "Unexpected or missing fields" }
        val category = objectValue.get("category")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            ?: throw IllegalArgumentException("category must be string")
        require(category in ALL_LABELS) { "Unsupported category" }
        val confidence = objectValue.get("confidence")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble
            ?: throw IllegalArgumentException("confidence must be number")
        require(confidence.isFinite() && confidence in 0.0..1.0) { "confidence out of range" }
        val reason = objectValue.get("reason")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            ?: throw IllegalArgumentException("reason must be string")
        require(reason.isNotBlank() && reason.length <= MAX_REASON_LENGTH && reason.any { it.code in 0x0400..0x04ff }) { "reason must be bounded Russian text" }
        return FallbackResponse(category, confidence, reason)
    }

    fun writeReports(summary: RunSummary, reportDir: Path) {
        Files.createDirectories(reportDir)
        Files.newBufferedWriter(reportDir.resolve("results.jsonl")).use { writer ->
            summary.results.forEach { writer.append(gson.toJson(it)).appendLine() }
        }
        Files.writeString(reportDir.resolve("summary.txt"), summaryText(summary))
    }

    fun summaryText(summary: RunSummary): String {
        val rows = summary.results
        val latencies = rows.map { it.totalLatencyMs }.sorted()
        val micro = rows.filter { it.actualRoute == "MICRO" }
        val fallback = rows.filter { it.actualRoute == "FALLBACK" }
        val lines = mutableListOf(
            "Examples: ${rows.size}",
            "Handled by micro-model: ${micro.size}",
            "Fallback count: ${fallback.size}",
            "Micro coverage: ${ratio(micro.size, rows.size)}",
            "Large LLM calls: ${summary.largeModelCalls}",
            "Average latency: ${format(summary.averageLatencyMs)} ms",
            "Median latency: ${format(median(latencies))} ms",
            "P95 latency: ${format(percentile95(latencies))} ms",
            "Average micro-only latency: ${format(micro.map { it.totalLatencyMs }.averageOrZero())} ms",
            "Average fallback latency: ${format(fallback.map { it.fallbackLatencyMs }.averageOrZero())} ms",
            "Overall label accuracy: ${ratio(rows.count { it.correctLabel }, rows.size)}",
            "Route accuracy: ${ratio(rows.count { it.correctRoute }, rows.size)}",
            "Micro accepted accuracy: ${ratio(micro.count { it.correctLabel }, micro.size)}",
            "Incorrect micro accepts: ${micro.count { !it.correctLabel }} (${ratio(micro.count { !it.correctLabel }, micro.size)})",
            "Fallback accuracy: ${ratio(fallback.count { it.correctLabel }, fallback.size)}"
        )
        listOf("simple", "boundary", "complex-noisy").forEach { group ->
            val groupRows = rows.filter { it.group == group }
            lines += "Group $group: count=${groupRows.size}, label_accuracy=${ratio(groupRows.count { it.correctLabel }, groupRows.size)}, route_accuracy=${ratio(groupRows.count { it.correctRoute }, groupRows.size)}"
        }
        FALLBACK_REASONS.forEach { reason -> lines += "Fallback reason $reason: ${rows.count { it.fallbackReason == reason }}" }
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun evaluate(case: TestCase, references: Map<String, List<List<Double>>>?, setupError: String?): ResultRow {
        val totalStart = System.nanoTime()
        val microStart = totalStart
        var decision = MicroDecision(null, null, null, null, "UNSURE", "PROTOTYPE_INITIALIZATION_ERROR")
        var error: String? = setupError?.let { "micro: $it" }
        if (references != null) {
            try {
                val query = validateBatch(gateway.embed(listOf(case.input)), 1).single()
                decision = decide(query, references)
            } catch (failure: OllamaTransportException) {
                decision = decision.copy(fallbackReason = "EMBEDDING_ERROR")
                error = "micro: ${failure.message}"
            } catch (invalid: IllegalArgumentException) {
                decision = decision.copy(fallbackReason = "INVALID_VECTOR")
                error = "micro: ${invalid.message}"
            } catch (failure: Exception) {
                decision = decision.copy(fallbackReason = "EMBEDDING_ERROR")
                error = "micro: ${failure.message}"
            }
        }
        val microLatency = elapsedMs(microStart)
        val actualRoute = if (decision.status == "OK") "MICRO" else "FALLBACK"
        var finalLabel = decision.label.takeIf { actualRoute == "MICRO" }
        var fallbackLatency = 0.0
        var calls = 0
        if (actualRoute == "FALLBACK") {
            val fallbackStart = System.nanoTime()
            try {
                calls++
                val first = gateway.generate(case.input, false)
                finalLabel = try { parseFallback(first).category } catch (_: IllegalArgumentException) {
                    calls++
                    parseFallback(gateway.generate(case.input, true)).category
                }
            } catch (failure: Exception) {
                error = listOfNotNull(error, "fallback: ${failure.message}").joinToString("; ")
            }
            fallbackLatency = elapsedMs(fallbackStart)
        }
        val totalLatency = elapsedMs(totalStart)
        return ResultRow(case.id, case.group, case.input, case.expectedLabel, case.expectedRoute,
            decision.label, decision.topScore, decision.secondScore, decision.margin, decision.status,
            actualRoute, finalLabel, finalLabel == case.expectedLabel, actualRoute == case.expectedRoute,
            decision.fallbackReason, microLatency, fallbackLatency, totalLatency, calls, error)
    }

    private fun validateBatch(vectors: List<List<Double>>, expected: Int): List<List<Double>> {
        require(vectors.size == expected && vectors.isNotEmpty()) { "Embedding batch size mismatch" }
        val dimensions = vectors.first().size
        require(dimensions > 0 && vectors.all { it.size == dimensions }) { "Embedding dimensions differ" }
        return vectors.map(::normalize)
    }

    private fun normalize(vector: List<Double>): List<Double> {
        require(vector.isNotEmpty() && vector.all { it.isFinite() }) { "Embedding must contain finite values" }
        val norm = sqrt(vector.sumOf { it * it })
        require(norm.isFinite() && norm > 0.0) { "Embedding must be non-zero" }
        return vector.map { it / norm }
    }

    private fun cosine(left: List<Double>, right: List<Double>): Double {
        val normalizedRight = normalize(right)
        require(left.size == normalizedRight.size) { "Embedding dimensions differ" }
        return left.indices.sumOf { left[it] * normalizedRight[it] }
    }

    private fun elapsedMs(start: Long) = (System.nanoTime() - start) / 1_000_000.0
    private fun List<Double>.averageOrZero() = if (isEmpty()) 0.0 else average()
    private fun ratio(numerator: Int, denominator: Int) = if (denominator == 0) "0.0000" else String.format(Locale.ROOT, "%.4f", numerator.toDouble() / denominator)
    private fun format(value: Double) = String.format(Locale.ROOT, "%.3f", value)
    private fun median(values: List<Double>) = if (values.isEmpty()) 0.0 else if (values.size % 2 == 1) values[values.size / 2] else (values[values.size / 2 - 1] + values[values.size / 2]) / 2
    private fun percentile95(values: List<Double>) = if (values.isEmpty()) 0.0 else values[(ceil(values.size * .95).toInt() - 1).coerceAtLeast(0)]

    companion object {
        fun defaultGson(): Gson = GsonBuilder().serializeNulls().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    }
}
