package com.aiassistant.developer.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.io.path.writeText

class ConfigLoaderTest {
    @Test fun `uses hybrid model environment configuration`() {
        val root = Files.createTempDirectory("assistant-config")
        val config = ConfigLoader.load(arrayOf("--project-root=$root"), mapOf("OPENAI_API_KEY" to "test-key"))
        assertEquals("http://localhost:11434", config.embeddingBaseUrl)
        assertEquals("nomic-embed-text:latest", config.embeddingModel)
        assertEquals("https://api.openai.com/v1", config.openAiBaseUrl)
        assertEquals("gpt-4.1-mini", config.openAiModel)
        assertEquals(false, config.dryRun)
    }

    @Test fun `parses dry run flag`() {
        val root = Files.createTempDirectory("assistant-dry-run")
        val config = ConfigLoader.load(arrayOf("--project-root=$root", "--dry-run"), mapOf("OPENAI_API_KEY" to "test-key"))
        assertEquals(true, config.dryRun)
    }

    @Test fun `fails when OpenAI key is absent from environment and local properties`() {
        val root = Files.createTempDirectory("assistant-config-no-key")
        assertFailsWith<IllegalArgumentException> { ConfigLoader.load(arrayOf("--project-root=$root"), emptyMap()) }
    }

    @Test fun `loads OpenAI key from project local properties`() {
        val root = Files.createTempDirectory("assistant-local-properties")
        root.resolve("local.properties").writeText("OPENAI_API_KEY=local-key\n")
        val config = ConfigLoader.load(arrayOf("--project-root=$root"), emptyMap())
        assertEquals("local-key", config.openAiApiKey)
    }

    @Test fun `environment OpenAI key has priority over local properties`() {
        val root = Files.createTempDirectory("assistant-env-priority")
        root.resolve("local.properties").writeText("OPENAI_API_KEY=local-key\n")
        val config = ConfigLoader.load(arrayOf("--project-root=$root"), mapOf("OPENAI_API_KEY" to "environment-key"))
        assertEquals("environment-key", config.openAiApiKey)
    }
}
