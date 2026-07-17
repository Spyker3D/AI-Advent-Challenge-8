package com.aiassistant.developer.review

import com.aiassistant.developer.llm.LlmClient
import com.aiassistant.developer.mcp.GitReviewProvider
import com.aiassistant.rag.ProjectChunk
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PullRequestReviewServiceTest {
    @Test fun `diff and RAG enter prompt and OpenAI response is saved`() = runBlocking {
        val fixture = fixture(diff = "diff --git a/A.kt b/A.kt\n+danger()")
        fixture.service.execute(fixture.request)

        assertContains(fixture.llm.input, "+danger()")
        assertContains(fixture.llm.input, "Architecture rule from docs")
        assertEquals("<!-- ai-developer-review -->\n## AI Code Review", fixture.request.output.readText())
    }

    @Test fun `empty diff writes comment without OpenAI`() = runBlocking {
        val fixture = fixture(diff = "")
        val result = fixture.service.execute(fixture.request)
        assertContains(result, "нет анализируемых изменений")
        assertFalse(fixture.llm.called)
    }

    @Test fun `large diff is truncated and limitation is explicit`() = runBlocking {
        val fixture = fixture(diff = "x".repeat(200), maxDiffChars = 40)
        val result = fixture.service.execute(fixture.request)
        assertContains(fixture.llm.input, "Diff truncated: true")
        assertContains(result, "частичное ревью")
    }

    @Test fun `off mode creates review without empty RAG section`() = runBlocking {
        val fixture = fixture(diff = "+change", withRag = false)
        fixture.service.execute(fixture.request)
        assertFalse(fixture.llm.input.contains("Relevant project context"))
        assertTrue(Files.exists(fixture.request.output))
    }

    private fun fixture(diff: String, maxDiffChars: Int = 120_000, withRag: Boolean = true): Fixture {
        val root = Files.createTempDirectory("review-service")
        val chunk = ProjectChunk(
            id = "1", sourcePath = "docs/architecture.md", content = "Architecture rule from docs",
            fileExtension = "md", startLine = 1, endLine = 2, symbolName = null,
            contentHash = "hash", embedding = listOf(1f, 0f)
        )
        val git = object : GitReviewProvider {
            override fun changedFiles(baseRef: String, headRef: String) = "M\tA.kt"
            override fun diff(baseRef: String, headRef: String) = diff
        }
        val llm = CapturingLlm()
        val request = ReviewRequest("base", "head", root.resolve("build/ai-review.md"))
        val rag = if (withRag) ReviewRagContextProvider(RagMode.EXISTING, { true }, {}, { listOf(chunk) }, {})
        else ReviewRagContextProvider(RagMode.OFF, { error("must not check index") }, { error("must not update") },
            { error("must not search") }, {})
        return Fixture(PullRequestReviewService(git, rag, llm, maxDiffChars), request, llm)
    }

    private data class Fixture(val service: PullRequestReviewService, val request: ReviewRequest, val llm: CapturingLlm)

    private class CapturingLlm : LlmClient {
        var input = ""; var called = false
        override suspend fun generate(instructions: String, input: String): String {
            called = true; this.input = input
            return "<!-- ai-developer-review -->\n## AI Code Review"
        }
    }
}
