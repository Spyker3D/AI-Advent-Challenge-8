package com.aiassistant.developer.llm

import com.aiassistant.developer.mcp.GitBranchProvider
import com.aiassistant.rag.IndexStorage
import com.aiassistant.rag.ProjectRetriever
import java.nio.file.Path

class DeveloperAssistantService(
    private val root: Path,
    private val storage: IndexStorage,
    private val retriever: ProjectRetriever,
    private val git: GitBranchProvider,
    private val llm: LlmClient
) {
    suspend fun answer(question: String): String {
        require(question.isNotBlank()) { "Usage: /help <question>" }
        val chunks = retriever.retrieve(question, storage.load())
        val branch = git.currentBranch()
        val branchText = branch.branch ?: "unavailable"
        val context = chunks.joinToString("\n\n") { "SOURCE ${it.sourcePath}:${it.startLine}-${it.endLine}\n${it.content}" }
        val prompt = "Project root: $root\nCurrent Git branch: $branchText\n\nRetrieved context:\n$context\n\nQuestion: $question"
        val response = llm.generate(SYSTEM_PROMPT, prompt)
        val sources = chunks.joinToString("\n") { "- ${it.sourcePath}:${it.startLine}-${it.endLine}" }
        return buildString {
            appendLine(response)
            appendLine(); appendLine("Current branch: $branchText")
            if (branch.message != null) appendLine(branch.message)
            appendLine(); appendLine("Sources:"); append(sources.ifBlank { "- No relevant indexed sources found." })
        }
    }

    companion object {
        const val SYSTEM_PROMPT = """You are an assistant for a local software project. Answer only from the supplied project context. Do not invent files, classes, methods, dependencies, or architecture. If context is insufficient, say so. Always cite relative file paths and line numbers when available. Clearly separate confirmed facts from assumptions."""
    }
}
