package com.aiassistant.developer.cli

import com.aiassistant.developer.config.ConfigLoader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DiagnosticsReportTest {

    @Test
    fun `reports safe configuration without exposing API key`() {
        val root = Files.createTempDirectory("assistant-diagnostics")
        val secret = "diagnostic-secret-value"
        val config = ConfigLoader.load(
            arrayOf("--project-root=$root", "--generation-model=test-model"),
            mapOf("OPENAI_API_KEY" to secret)
        )
        val before = Files.list(root).use { it.count() }

        val report = DiagnosticsReport.create(config)

        assertContains(report, "Project root: $root")
        assertContains(report, "Generation model: test-model")
        assertContains(report, "OpenAI API key: configured")
        assertFalse(report.contains(secret))
        assertEquals(before, Files.list(root).use { it.count() })
    }

    @Test
    fun `diagnostics configuration allows an absent API key`() {
        val root = Files.createTempDirectory("assistant-diagnostics-no-key")
        val config = ConfigLoader.load(
            arrayOf("--project-root=$root"),
            emptyMap(),
            requireApiKey = false
        )

        assertContains(DiagnosticsReport.create(config), "OpenAI API key: not configured")
    }
}