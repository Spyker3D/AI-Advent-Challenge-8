package com.aiassistant.developer.agent

import com.aiassistant.developer.files.ProjectFileTools
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import com.aiassistant.developer.llm.LlmClient

class ProjectFileAgentIntegrationTest {
    @Test fun `agent finds usage reads files proposes diff and respects dry run and confirmation`() = runBlocking {
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

    @Test fun `goal dynamically selects OpenAI API and generates matching report`() = runBlocking {
        val root = Files.createTempDirectory("day34-openai")
        root.resolve("OpenAiClient.kt").writeText("class OpenAiClient // OpenAI API")
        root.resolve("ApiConfig.kt").writeText("const val URL = \"https://api.openai.com/v1\"")
        root.resolve("README.md").writeText("# Sample\nOpenAI")
        val result = ProjectFileAgent(root, ProjectFileTools(root), dryRun = true).execute("Найди все места, где проект использует OpenAI API, и создай отчёт") { false }
        assertContains(result.diff, "docs/generated/openai-api-usage.md")
        assertContains(result.diff, "OpenAiClient.kt")
    }

    @Test fun `usage goal accepts API OpenAi word order and punctuation`() = runBlocking {
        val root = Files.createTempDirectory("day34-openai-alias")
        root.resolve("OpenAiApi.kt").writeText("interface OpenAiApi")
        root.resolve("Client.kt").writeText("val api: OpenAiApi")
        root.resolve("README.md").writeText("# Sample")
        val misleadingLlm = object : LlmClient {
            override suspend fun generate(instructions: String, input: String) = error("Known alias must not call LLM")
        }
        val result = ProjectFileAgent(root, ProjectFileTools(root), true, misleadingLlm)
            .execute("Найди все места, где используется API OpenAi и создай отчет.") { false }
        assertContains(result.diff, "docs/generated/openai-api-usage.md")
        assertContains(result.diff, "OpenAiApi.kt")
    }

    @Test fun `documentation goal uses requested subject instead of support`() = runBlocking {
        val root = Files.createTempDirectory("day34-openai-doc")
        root.resolve("OpenAiClient.kt").writeText("class OpenAiClient // OpenAI API")
        root.resolve("ApiConfig.kt").writeText("const val OPENAI = \"https://api.openai.com/v1\"")
        root.resolve("README.md").writeText("# Sample")
        val result = ProjectFileAgent(root, ProjectFileTools(root), dryRun = true).execute("Изучи реализацию OpenAI API и обнови README, чтобы документация соответствовала коду") { false }
        assertContains(result.diff, "Current implementation: OpenAI API")
        assertFalse(result.diff.contains("support implementation"))
    }

    @Test fun `OpenAI documentation explains implementation rather than listing matches only`() = runBlocking {
        val root = Files.createTempDirectory("day34-openai-semantic-doc")
        fun file(path: String, text: String) { root.resolve(path).also { Files.createDirectories(it.parent); it.writeText(text) } }
        file("README.md", "# Sample")
        file("app/build.gradle.kts", "val openAiApiKey = localProperties.getProperty(\"OPENAI_API_KEY\")")
        file("app/src/main/java/com/aiassistant/di/AppModule.kt", "OpenAiAuthInterceptor(BuildConfig.OPENAI_API_KEY)")
        file("core/network/src/main/java/com/aiassistant/core/network/di/NetworkModule.kt", "val OPENAI_BASE_URL = \"https://api.openai.com/\"")
        file("core/network/src/main/java/com/aiassistant/core/network/api/OpenAiApi.kt", "@POST(\"v1/responses\") interface OpenAiApi")
        file("core/data/src/main/java/com/aiassistant/core/data/client/LlmClientImpl.kt", "class LlmClientImpl // OpenAI output_text HTTP 429")
        file("core/domain/src/main/java/com/aiassistant/core/domain/entity/ChatSettings.kt", "const val DEFAULT_OPENAI_MODEL = \"gpt-4.1-mini\"")
        val fake = object : LlmClient {
            override suspend fun generate(instructions: String, input: String): String =
                if (instructions.contains("Extract the software component")) """{"subject":"OpenAI API","searchTerms":["OpenAI","OPENAI"]}"""
                else """### Current implementation: OpenAI API

The application calls the OpenAI Responses API directly and uses `Authorization: Bearer <key>` (`OpenAiApi.kt:1`). HTTP 400, 401, 403, 429 and server failures are handled by the client (`LlmClientImpl.kt:1`)."""
        }
        val result = ProjectFileAgent(root, ProjectFileTools(root), dryRun = true, llm = fake).execute("Изучи реализацию OpenAI API и обнови README") { false }
        assertContains(result.diff, "calls the OpenAI Responses API directly")
        assertContains(result.diff, "Authorization: Bearer <key>")
        assertContains(result.diff, "HTTP 400, 401, 403, 429")
    }
}
