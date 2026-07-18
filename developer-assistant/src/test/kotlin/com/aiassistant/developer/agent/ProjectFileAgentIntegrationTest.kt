package com.aiassistant.developer.agent

import com.aiassistant.developer.files.ProjectFileTools
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.*

class ProjectFileAgentIntegrationTest {
    @Test fun `agent finds usage reads files proposes diff and respects dry run and confirmation`() {
        val root = Files.createTempDirectory("day34-agent")
        root.resolve("SampleService.kt").writeText("class SupportAssistantService { fun ask() = Unit }")
        root.resolve("SampleScreen.kt").writeText("fun screen(service: SupportAssistantService) = service.ask()")
        root.resolve("README.md").writeText("# Sample\nSupportAssistantService")
        root.resolve("tickets.json").writeText("[]")
        val output = root.resolve("docs/generated/support-assistant-usage.md")

        val dry = ProjectFileAgent(root, ProjectFileTools(root), dryRun = true).execute("Найди все использования SupportAssistantService и создай отчёт") { error("must not confirm") }
        assertContains(dry.diff, "support-assistant-usage.md"); assertFalse(Files.exists(output)); assertTrue(dry.iterations >= 6)

        var asked = false
        val saved = ProjectFileAgent(root, ProjectFileTools(root), dryRun = false).execute("Найди все использования SupportAssistantService и создай отчёт") { asked = true; true }
        assertTrue(asked); assertEquals(listOf("docs/generated/support-assistant-usage.md"), saved.changedFiles)
        assertContains(output.readText(), "SampleScreen.kt")
    }

    @Test fun `goal dynamically selects OpenAI API and generates matching report`() {
        val root = Files.createTempDirectory("day34-openai")
        root.resolve("OpenAiClient.kt").writeText("class OpenAiClient // OpenAI API")
        root.resolve("ApiConfig.kt").writeText("const val URL = \"https://api.openai.com/v1\"")
        root.resolve("README.md").writeText("# Sample\nOpenAI")
        val result = ProjectFileAgent(root, ProjectFileTools(root), dryRun = true).execute("Найди все места, где проект использует OpenAI API, и создай отчёт") { false }
        assertContains(result.diff, "docs/generated/openai-api-usage.md")
        assertContains(result.diff, "OpenAiClient.kt")
    }

    @Test fun `documentation goal uses requested subject instead of support`() {
        val root = Files.createTempDirectory("day34-openai-doc")
        root.resolve("OpenAiClient.kt").writeText("class OpenAiClient // OpenAI API")
        root.resolve("ApiConfig.kt").writeText("const val OPENAI = \"https://api.openai.com/v1\"")
        root.resolve("README.md").writeText("# Sample")
        val result = ProjectFileAgent(root, ProjectFileTools(root), dryRun = true).execute("Изучи реализацию OpenAI API и обнови README, чтобы документация соответствовала коду") { false }
        assertContains(result.diff, "Фактическая реализация: OpenAI API")
        assertFalse(result.diff.contains("реализация поддержки"))
    }
}
