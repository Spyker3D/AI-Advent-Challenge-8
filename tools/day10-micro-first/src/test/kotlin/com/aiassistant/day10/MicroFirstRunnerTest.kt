package com.aiassistant.day10

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MicroFirstRunnerTest {
    @Test
    fun `loads existing datasets with exact distribution and no verbatim copies`() {
        val runner = MicroFirstRunner(FakeGateway())
        val root = Path.of("").toAbsolutePath().resolve("data")
        val prototypes = runner.loadPrototypes(root.resolve("prototypes.json"))
        val cases = runner.loadCases(root.resolve("test_cases.jsonl"))
        assertTrue(prototypes.values.all { it.distinct().size >= 8 })
        assertEquals(30, cases.size)
        assertEquals(mapOf("boundary" to 10, "complex-noisy" to 10, "simple" to 10), cases.groupingBy { it.group }.eachCount().toSortedMap())
        assertTrue(prototypes.values.flatten().map { it.lowercase() }.toSet().intersect(cases.map { it.input.lowercase() }.toSet()).isEmpty())
    }

    @Test
    fun `uses maximum prototype similarity and unchanged thresholds`() {
        val runner = MicroFirstRunner(FakeGateway())
        val decision = runner.decide(
            listOf(1.0, 0.0),
            mapOf(
                "NETWORK_UNAVAILABLE" to listOf(listOf(1.0, 0.0), listOf(0.0, 1.0)),
                "OPENAI_TIMEOUT" to listOf(listOf(.8, .6))
            )
        )
        assertEquals("NETWORK_UNAVAILABLE", decision.label)
        assertEquals(1.0, decision.topScore)
        assertEquals("OK", decision.status)
        assertEquals(.70, MIN_SCORE)
        assertEquals(.06, MIN_MARGIN)
    }

    @Test
    fun `fallback parser accepts exactly three bounded Russian fields`() {
        val runner = MicroFirstRunner(FakeGateway())
        val valid = """{"category":"OPENAI_TIMEOUT","confidence":0.8,"reason":"Истёк срок ожидания"}"""
        assertEquals("OPENAI_TIMEOUT", runner.parseFallback(valid).category)
        assertFailsWith<IllegalArgumentException> { runner.parseFallback("""{"category":"BAD","confidence":0.8,"reason":"Ошибка"}""") }
        assertFailsWith<IllegalArgumentException> { runner.parseFallback("""{"category":"AMBIGUOUS","confidence":1.1,"reason":"Неясно"}""") }
        assertFailsWith<IllegalArgumentException> { runner.parseFallback("""{"category":"AMBIGUOUS","confidence":0.4,"reason":"English"}""") }
        assertFailsWith<IllegalArgumentException> { runner.parseFallback("""{"category":"AMBIGUOUS","confidence":0.4,"reason":"${"я".repeat(161)}"}""") }
        assertFailsWith<IllegalArgumentException> { runner.parseFallback("""{"category":"AMBIGUOUS","confidence":0.4,"reason":"Неясно","extra":1}""") }
    }

    @Test
    fun `valid fallback uses one call and invalid structure receives one correction retry`() {
        val prototypes = LABELS.associateWith { label -> List(8) { "$label-$it" } }
        val case = TestCase("x", "boundary", "Попробуйте позже", "AMBIGUOUS", "FALLBACK")
        val embeddings = buildList {
            add(List(40) { index -> if (index % 2 == 0) listOf(1.0, 0.0) else listOf(0.0, 1.0) })
            add(listOf(listOf(1.0, 1.0)))
        }
        val validGateway = FakeGateway(embeddings.toMutableList(), mutableListOf("""{"category":"AMBIGUOUS","confidence":0.8,"reason":"Причина не определена"}"""))
        val valid = MicroFirstRunner(validGateway).run(prototypes, listOf(case))
        assertEquals(1, valid.largeModelCalls)
        assertEquals(listOf(false), validGateway.corrections)

        val retryGateway = FakeGateway(embeddings.toMutableList(), mutableListOf("{}", """{"category":"AMBIGUOUS","confidence":0.8,"reason":"Причина не определена"}"""))
        val retried = MicroFirstRunner(retryGateway).run(prototypes, listOf(case))
        assertEquals(2, retried.largeModelCalls)
        assertEquals(listOf(false, true), retryGateway.corrections)
    }

    @Test
    fun `query transport failure maps to embedding error instead of invalid vector`() {
        val prototypes = LABELS.associateWith { label -> List(8) { "$label-$it" } }
        val embeddings = mutableListOf(List(40) { listOf(1.0, 0.0) })
        val gateway = FakeGateway(
            embeddingResponses = embeddings,
            generatedResponses = mutableListOf("""{"category":"AMBIGUOUS","confidence":0.5,"reason":"Причина не определена"}"""),
            failOnEmbedCalls = setOf(2)
        )
        val result = MicroFirstRunner(gateway).run(prototypes, listOf(TestCase("x", "boundary", "Ошибка", "AMBIGUOUS", "FALLBACK"))).results.single()
        assertEquals("EMBEDDING_ERROR", result.fallbackReason)
        assertEquals(1, result.largeLlmCalls)
        assertTrue(result.error?.contains("HTTP 503") == true)
    }

    @Test
    fun `throwing generate attempts are counted and later cases continue`() {
        val prototypes = LABELS.associateWith { label -> List(8) { "$label-$it" } }
        val embeddings = mutableListOf<List<List<Double>>>(List(40) { listOf(1.0, 0.0) }).apply {
            repeat(3) { add(listOf(listOf(1.0, 1.0))) }
        }
        val valid = """{"category":"AMBIGUOUS","confidence":0.5,"reason":"Причина не определена"}"""
        val gateway = FakeGateway(embeddings, mutableListOf("{}", valid), failOnGenerateCalls = setOf(1, 3))
        val cases = (1..3).map { TestCase("x$it", "boundary", "Ошибка $it", "AMBIGUOUS", "FALLBACK") }
        val summary = MicroFirstRunner(gateway).run(prototypes, cases)
        assertEquals(listOf(1, 2, 1), summary.results.map { it.largeLlmCalls })
        assertEquals(4, summary.largeModelCalls)
        assertTrue(summary.results[0].error?.contains("generate failed") == true)
        assertTrue(summary.results[1].error?.contains("generate failed") == true)
        assertEquals("AMBIGUOUS", summary.results[2].finalLabel)
        assertEquals(listOf(false, false, true, false), gateway.corrections)
    }

    @Test
    fun `writes exact result fields and summary metrics`() {
        val runner = MicroFirstRunner(FakeGateway())
        val row = ResultRow("x", "simple", "input", "OPENAI_TIMEOUT", "MICRO", "OPENAI_TIMEOUT", .9, .1, .8, "OK", "MICRO", "OPENAI_TIMEOUT", true, true, null, 1.0, 0.0, 1.0, 0, null)
        val summary = RunSummary(listOf(row), 1, 1, 0, 0, 1.0)
        val directory = Files.createTempDirectory("day10-report")
        runner.writeReports(summary, directory)
        val json = JsonParser.parseString(Files.readString(directory.resolve("results.jsonl"))).asJsonObject
        assertEquals(20, json.keySet().size)
        assertEquals("OPENAI_TIMEOUT", json.get("final_label").asString)
        val text = Files.readString(directory.resolve("summary.txt"))
        assertTrue(text.contains("Examples: 1"))
        assertTrue(text.contains("Handled by micro-model: 1"))
        assertTrue(text.contains("Overall label accuracy: 1.0000"))
    }
}

private class FakeGateway(
    private val embeddingResponses: MutableList<List<List<Double>>> = mutableListOf(),
    private val generatedResponses: MutableList<String> = mutableListOf(),
    private val failOnEmbedCalls: Set<Int> = emptySet(),
    private val failOnGenerateCalls: Set<Int> = emptySet()
) : OllamaGateway {
    val corrections = mutableListOf<Boolean>()
    private var embedCalls = 0
    private var generateCalls = 0
    override fun embed(texts: List<String>): List<List<Double>> {
        embedCalls++
        if (embedCalls in failOnEmbedCalls) throw OllamaTransportException("/api/embed returned HTTP 503")
        return embeddingResponses.removeFirst()
    }
    override fun generate(input: String, correction: Boolean): String {
        generateCalls++
        corrections += correction
        if (generateCalls in failOnGenerateCalls) throw OllamaTransportException("generate failed")
        return generatedResponses.removeFirst()
    }
}
