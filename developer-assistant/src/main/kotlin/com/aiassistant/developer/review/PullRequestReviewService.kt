package com.aiassistant.developer.review

import com.aiassistant.developer.llm.LlmClient
import com.aiassistant.developer.mcp.GitReviewProvider
import com.aiassistant.rag.IndexStorage
import com.aiassistant.rag.ProjectRetriever
import java.nio.file.Files
import java.nio.file.Path

data class ReviewRequest(val baseRef: String, val headRef: String, val output: Path)

class PullRequestReviewService(
    private val git: GitReviewProvider,
    private val storage: IndexStorage,
    private val retriever: ProjectRetriever,
    private val llm: LlmClient,
    private val maxDiffChars: Int = 120_000
) {
    suspend fun execute(request: ReviewRequest): String {
        val changedFiles = git.changedFiles(request.baseRef, request.headRef)
        val fullDiff = git.diff(request.baseRef, request.headRef)
        val review = if (fullDiff.isBlank()) emptyDiffReview(request) else {
            val truncated = fullDiff.length > maxDiffChars
            val diff = fullDiff.take(maxDiffChars)
            val query = "Code review architecture API models errors tests for changed files:\n$changedFiles"
            val chunks = retriever.retrieve(query, storage.load())
            val rag = chunks.joinToString("\n\n") {
                "SOURCE ${it.sourcePath}:${it.startLine ?: "?"}-${it.endLine ?: "?"}\n${it.content}"
            }
            val prompt = buildPrompt(request, changedFiles, diff, rag, truncated, fullDiff.length)
            ensureMarker(llm.generate(SYSTEM_PROMPT, prompt), truncated)
        }
        request.output.toAbsolutePath().parent?.let(Files::createDirectories)
        Files.writeString(request.output, review)
        return review
    }

    internal fun buildPrompt(
        request: ReviewRequest,
        changedFiles: String,
        diff: String,
        rag: String,
        truncated: Boolean,
        originalDiffChars: Int
    ): String = """Base SHA/ref: ${request.baseRef}
Head SHA/ref: ${request.headRef}
Changed files (MCP get_changed_files):
$changedFiles

Unified diff (MCP get_git_diff):
$diff

Relevant project RAG context:
$rag

Diff truncated: $truncated${if (truncated) " (reviewed $maxDiffChars of $originalDiffChars characters)" else ""}
Produce the required Markdown PR review. Findings must be supported by the diff or RAG context."""

    private fun ensureMarker(response: String, truncated: Boolean): String = buildString {
        if (!response.contains(MARKER)) appendLine(MARKER)
        append(response.trim())
        if (truncated && !response.contains("частич", ignoreCase = true) && !response.contains("truncat", ignoreCase = true)) {
            appendLine(); appendLine(); appendLine("### Ограничения ревью")
            append("- Diff превышает лимит; выполнено частичное ревью первых $maxDiffChars символов.")
        }
    }

    private fun emptyDiffReview(request: ReviewRequest) = """$MARKER

## AI Code Review

### Итог

Между `${request.baseRef}` и `${request.headRef}` нет анализируемых изменений.

### Ограничения ревью

- Unified diff пуст; вызов OpenAI не выполнялся.
""".trim()

    companion object {
        const val MARKER = "<!-- ai-developer-review -->"
        const val SYSTEM_PROMPT = """You are a strict senior code reviewer for this repository. Review only evidence present in the unified diff and supplied RAG context; never invent findings. Return Markdown beginning with <!-- ai-developer-review --> and heading ## AI Code Review. Include: Итог with risk, Потенциальные баги, Архитектурные проблемы, Рекомендации, Что сделано хорошо, Ограничения ревью. For every finding provide severity from CRITICAL/HIGH/MEDIUM/LOW/INFO, file path, changed diff lines when available, explanation, and a concrete recommendation. Check security, coroutine/lifecycle, nullability, error handling, API/model compatibility, dependencies, and missing tests. If a section has no supported findings, explicitly say so."""
    }
}
