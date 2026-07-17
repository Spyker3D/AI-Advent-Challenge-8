package com.aiassistant.developer.review

import java.nio.file.Path
import java.nio.file.Paths

enum class RagMode {
    OFF,
    EXISTING,
    UPDATE;

    companion object {
        fun parse(value: String): RagMode = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "Invalid --rag-mode value: $value.\nSupported values: off, existing, update."
            )
    }
}

data class ReviewRequest(
    val baseRef: String,
    val headRef: String,
    val output: Path,
    val ragMode: RagMode = RagMode.UPDATE
)

object ReviewCommandParser {
    fun parse(args: Array<String>, projectRoot: Path): ReviewRequest {
        require(args.firstOrNull() == "review-pr") { "Expected review-pr command." }
        val options = args.drop(1).filter { it.startsWith("--") }.associate {
            val pair = it.removePrefix("--").split('=', limit = 2)
            pair[0] to pair.getOrElse(1) { "" }
        }
        fun required(name: String): String = options[name]?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("review-pr requires --$name=<value>")
        val rawOutput = Paths.get(required("output"))
        val output = if (rawOutput.isAbsolute) rawOutput else projectRoot.resolve(rawOutput)
        return ReviewRequest(
            baseRef = required("base-ref"),
            headRef = required("head-ref"),
            output = output.normalize(),
            ragMode = options["rag-mode"]?.takeIf(String::isNotBlank)?.let(RagMode::parse) ?: RagMode.UPDATE
        )
    }
}
