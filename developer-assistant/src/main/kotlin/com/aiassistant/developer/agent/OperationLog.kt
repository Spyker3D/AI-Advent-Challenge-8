package com.aiassistant.developer.agent

import com.google.gson.GsonBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant

data class OperationRecord(
    val timestamp: String = Instant.now().toString(), val goal: String, val toolsUsed: List<String>,
    val filesRead: List<String>, val filesChanged: List<String>, val status: String, val iterations: Int
)

private data class OperationHistoryRecord(
    val timestamp: String,
    val toolsUsed: List<String>,
    val filesRead: List<String>,
    val filesChanged: List<String>,
    val status: String,
    val iterations: Int
)

class OperationLogger(private val root: Path) {
    private val gson = GsonBuilder().create()
    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    fun save(record: OperationRecord) {
        val directory = root.resolve("developer-assistant/logs")
        Files.createDirectories(directory)
        appendHistory(directory, record)
        replaceLastOperation(directory, record)
    }

    private fun appendHistory(directory: Path, record: OperationRecord) {
        val history = OperationHistoryRecord(
            timestamp = record.timestamp,
            toolsUsed = record.toolsUsed,
            filesRead = record.filesRead,
            filesChanged = record.filesChanged,
            status = record.status,
            iterations = record.iterations
        )
        Files.writeString(
            directory.resolve("operations.jsonl"),
            gson.toJson(history) + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }

    private fun replaceLastOperation(directory: Path, record: OperationRecord) {
        val target = directory.resolve("last-operation.json")
        val temp = Files.createTempFile(directory, ".operation-", ".tmp")
        Files.writeString(temp, prettyGson.toJson(record), StandardCharsets.UTF_8)
        runCatching { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            .getOrElse { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING) }
    }
}