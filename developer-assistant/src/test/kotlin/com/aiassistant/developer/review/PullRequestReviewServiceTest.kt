package com.aiassistant.developer.review

import com.aiassistant.developer.llm.LlmClient
import com.aiassistant.developer.mcp.GitReviewProvider
import com.aiassistant.rag.EmbeddingClient
import com.aiassistant.rag.IndexStorage
import com.aiassistant.rag.LocalVectorIndex
import com.aiassistant.rag.ProjectChunk
import com.aiassistant.rag.ProjectRetriever
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private fun fixture(diff: String, maxDiffChars: Int = 120_000): Fixture {
        val root = Files.createTempDirectory("review-service")
        val storage = IndexStorage(root.resolve("index.json"))
        storage.save(LocalVectorIndex(chunks = listOf(ProjectChunk(
            id = "1", sourcePath = "docs/architecture.md", content = "Architecture rule from docs",
            fileExtension = "md", startLine = 1, endLine = 2, symbolName = null,
            contentHash = "hash", embedding = listOf(1f, 0f)
        ))))
        val git = object : GitReviewProvider {
            override fun changedFiles(baseRef: String, headRef: String) = "M\tA.kt"
            override fun diff(baseRef: String, headRef: String) = diff
        }
        val llm = CapturingLlm()
        val retriever = ProjectRetriever(object : EmbeddingClient {
            override suspend fun embed(texts: List<String>) = texts.map { listOf(1f, 0f) }
        })
        val request = ReviewRequest("base", "head", root.resolve("build/ai-review.md"))
        return Fixture(PullRequestReviewService(git, storage, retriever, llm, maxDiffChars), request, llm)
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
