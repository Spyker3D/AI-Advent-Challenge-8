package com.aiassistant.developer.agent

import com.google.gson.GsonBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

data class OperationRecord(
    val timestamp: String = Instant.now().toString(), val goal: String, val toolsUsed: List<String>,
    val filesRead: List<String>, val filesChanged: List<String>, val status: String, val iterations: Int
)

class OperationLogger(private val root: Path) {
    fun save(record: OperationRecord) {
        val directory = root.resolve("developer-assistant/logs")
        Files.createDirectories(directory)
        val target = directory.resolve("last-operation.json")
        val temp = Files.createTempFile(directory, ".operation-", ".tmp")
        Files.writeString(temp, GsonBuilder().setPrettyPrinting().create().toJson(record), StandardCharsets.UTF_8)
        runCatching { Files.move(temp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
            .getOrElse { Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
    }
}
