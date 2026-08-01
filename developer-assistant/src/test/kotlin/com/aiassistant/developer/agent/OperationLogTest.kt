package com.aiassistant.developer.agent

import com.google.gson.JsonParser
import java.nio.file.Files
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OperationLogTest {

    @Test
    fun `overwrites last operation and appends every operation to history`() {
        val root = Files.createTempDirectory("operation-log")
        val logger = OperationLogger(root)
        val first = record(goal = "first", status = "completed", iterations = 1)
        val second = record(goal = "second", status = "failed", iterations = 2)

        logger.save(first)
        logger.save(second)

        val directory = root.resolve("developer-assistant/logs")
        val last = JsonParser.parseString(directory.resolve("last-operation.json").readText()).asJsonObject
        assertEquals("second", last.get("goal").asString)
        val history = directory.resolve("operations.jsonl").readLines()
        assertEquals(2, history.size)
        assertEquals("completed", JsonParser.parseString(history[0]).asJsonObject.get("status").asString)
        assertEquals("failed", JsonParser.parseString(history[1]).asJsonObject.get("status").asString)
    }

    @Test
    fun `history excludes goal and file contents`() {
        val root = Files.createTempDirectory("operation-log")
        val secret = "secret-file-content-123"

        OperationLogger(root).save(record(goal = secret, status = "completed", iterations = 1))

        val history = root.resolve("developer-assistant/logs/operations.jsonl").readText()
        assertFalse(history.contains(secret))
        assertFalse(JsonParser.parseString(history.trim()).asJsonObject.has("goal"))
        assertTrue(history.endsWith(System.lineSeparator()))
    }

    private fun record(goal: String, status: String, iterations: Int) = OperationRecord(
        timestamp = "2026-07-27T00:00:00Z",
        goal = goal,
        toolsUsed = listOf("read_file"),
        filesRead = listOf("README.md"),
        filesChanged = listOf("docs/report.md"),
        status = status,
        iterations = iterations
    )
}