package com.aiassistant.developer

import com.aiassistant.developer.cli.CommandLoop
import com.aiassistant.developer.config.ConfigLoader
import com.aiassistant.developer.llm.DeveloperAssistantService
import com.aiassistant.developer.llm.OpenAiConfig
import com.aiassistant.developer.llm.OpenAiResponsesClient
import com.aiassistant.developer.mcp.GitMcpClient
import com.aiassistant.rag.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    // Gradle daemons can retain the Windows console encoding they were started
    // with even after `chcp 65001`. Make this interactive CLI consistently emit
    // UTF-8 so Russian questions, answers, and labels are not corrupted.
    System.setOut(PrintStream(System.out, true, StandardCharsets.UTF_8))
    System.setErr(PrintStream(System.err, true, StandardCharsets.UTF_8))

    val config = try { ConfigLoader.load(args) } catch (error: Exception) {
        System.err.println("Error: ${error.message}"); exitProcess(2)
    }
    val stateDir = config.projectRoot.resolve(".developer-assistant")
    val indexPath = stateDir.resolve("index.json")
    val manifestPath = stateDir.resolve("manifest.json")
    val indexStorage = IndexStorage(indexPath)
    val manifestStorage = ManifestStorage(manifestPath)
    val embedding = OllamaEmbeddingClient(OllamaEmbeddingConfig(config.embeddingBaseUrl, config.embeddingModel))
    val git = GitMcpClient(config.mcpUrl)
    val indexer = ProjectIndexer(
        config.projectRoot,
        ProjectScanner(ScannerConfig(maxFileSizeBytes = config.maxFileSizeBytes)),
        ProjectChunker(config.chunkSize, config.chunkOverlap), embedding, "ollama", config.embeddingModel, indexStorage, manifestStorage
    )

    println("Developer Assistant\n")
    println("Project: ${config.projectRoot}")
    if (!Files.exists(indexPath)) println("Index not found.\nBuilding project index...") else println("Scanning project...")
    try {
        val update = runBlocking { indexer.update(progress = ::println) }
        printUpdate(update)
    } catch (error: Exception) {
        System.err.println("Index update failed: ${error.message}")
        if (config.debug) error.printStackTrace()
        if (!Files.exists(indexPath)) println("/help will be unavailable until indexing succeeds.")
    }
    val branch = git.currentBranch()
    println("Branch: ${branch.branch ?: "unavailable"}")
    println("MCP: ${if (branch.connected) "connected" else "disconnected"}")
    println("\nCommands:\n  /help <question>\n  /status\n  /reindex\n  /exit\n")

    val service = DeveloperAssistantService(config.projectRoot, indexStorage, ProjectRetriever(embedding, config.topK), git,
        OpenAiResponsesClient(OpenAiConfig(config.openAiBaseUrl, config.openAiModel, config.openAiApiKey)))
    val loop = CommandLoop(
        BufferedReader(InputStreamReader(System.`in`, StandardCharsets.UTF_8)),
        PrintWriter(System.out, true, StandardCharsets.UTF_8),
        { question -> runBlocking { service.answer(question) } }, {
        val manifest = manifestStorage.load(); val chunks = indexStorage.load(); val currentBranch = git.currentBranch()
        """Project: ${config.projectRoot}
Branch: ${currentBranch.branch ?: "unavailable"}
Indexed files: ${manifest.files.size}
Chunks: ${chunks.size}
Last indexed: ${formatTime(manifest.lastIndexedEpochMillis)}
Index path: $indexPath
MCP status: ${if (currentBranch.connected) "connected" else "disconnected"}
Embedding service: ${if (runBlocking { runCatching { embedding.embed(listOf("health check")) }.isSuccess }) "connected" else "disconnected"}"""
    }, {
        println("Force rebuilding project index...")
        val result = runBlocking { indexer.update(force = true, progress = ::println) }
        updateText(result)
    }, config.debug)
    loop.run()
}

private fun printUpdate(update: IndexUpdate) = println(updateText(update))
private fun updateText(u: IndexUpdate): String = if (u.newFiles + u.changedFiles + u.deletedFiles == 0) {
    "Files found: ${u.filesFound}\nReused embeddings: ${u.reusedChunks} chunks\nIndex is up to date."
} else """Files found: ${u.filesFound}
New files: ${u.newFiles}
Changed files: ${u.changedFiles}
Deleted files: ${u.deletedFiles}
Unchanged files: ${u.unchangedFiles}
Generating embeddings: ${u.generatedChunks} chunks
Reused embeddings: ${u.reusedChunks} chunks
Index updated."""

private fun formatTime(epoch: Long): String = if (epoch <= 0) "never" else DateTimeFormatter.ISO_LOCAL_DATE_TIME
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epoch))
