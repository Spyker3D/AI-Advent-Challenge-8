package com.aiassistant.day10

import java.nio.file.Path
import java.util.Locale

fun main(args: Array<String>) {
    val baseUrl = args.firstOrNull { it.startsWith("--base-url=") }?.substringAfter('=') ?: "http://127.0.0.1:11434"
    val root = Path.of("").toAbsolutePath()
    val tool = root.resolve("tools/day10-micro-first")
    val runner = MicroFirstRunner(OllamaClient(baseUrl = baseUrl))
    val summary = runner.run(
        runner.loadPrototypes(tool.resolve("data/prototypes.json")),
        runner.loadCases(tool.resolve("data/test_cases.jsonl"))
    )
    runner.writeReports(summary, tool.resolve("reports"))
    println("Total requests: ${summary.totalRequests}")
    println("Micro handled: ${summary.microHandled}")
    println("Fallback handled: ${summary.fallbackHandled}")
    println("Large model calls: ${summary.largeModelCalls}")
    println("Average latency: ${String.format(Locale.ROOT, "%.3f", summary.averageLatencyMs)} ms")
    ALL_LABELS.forEach { label ->
        val rows = summary.results.filter { it.expectedLabel == label }
        val correct = rows.count { it.correctLabel }
        val accuracy = if (rows.isEmpty()) 0.0 else correct.toDouble() / rows.size
        println("$label: total=${rows.size}, correct=$correct, accuracy=${String.format(Locale.ROOT, "%.4f", accuracy)}")
    }
}
