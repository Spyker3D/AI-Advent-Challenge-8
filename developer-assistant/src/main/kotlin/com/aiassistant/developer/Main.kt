package com.aiassistant.developer

import com.aiassistant.developer.cli.CommandLoop
import com.aiassistant.developer.config.ConfigLoader
import com.aiassistant.developer.llm.DeveloperAssistantService
import com.aiassistant.developer.llm.OpenAiConfig
import com.aiassistant.developer.llm.OpenAiResponsesClient
import com.aiassistant.developer.mcp.GitMcpClient
import com.aiassistant.developer.review.PullRequestReviewService
import com.aiassistant.developer.review.ReviewCommandParser
import com.aiassistant.developer.review.ReviewRagContextProvider
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

    val reviewMode = args.firstOrNull() == "review-pr"
    val config = try { ConfigLoader.load(args) } catch (error: Exception) {
        System.err.println("Error: ${error.message}"); exitProcess(2)
    }
    if (args.firstOrNull() == "index-support-knowledge") {
        val options = args.filter { it.startsWith("--") }.associate { val p = it.removePrefix("--").split('=', limit = 2); p[0] to p.getOrElse(1) { "" } }
        val knowledgeRoot = config.projectRoot.resolve(options["knowledge-root"] ?: "support-knowledge").normalize()
        require(knowledgeRoot.startsWith(config.projectRoot) && Files.isDirectory(knowledgeRoot)) { "Support knowledge directory does not exist or is outside project root: $knowledgeRoot" }
        val state = config.projectRoot.resolve(".support-assistant")
        val storage = IndexStorage(state.resolve("index.json")); val manifest = ManifestStorage(state.resolve("manifest.json"))
        val embedding = OllamaEmbeddingClient(OllamaEmbeddingConfig(config.embeddingBaseUrl, config.embeddingModel))
        val indexer = ProjectIndexer(knowledgeRoot, ProjectScanner(ScannerConfig(extensions = setOf("md"), maxFileSizeBytes = config.maxFileSizeBytes)), ProjectChunker(config.chunkSize, config.chunkOverlap), embedding, "ollama", config.embeddingModel, storage, manifest)
        try { val update = runBlocking { indexer.update(progress = ::println) }; println("Support documents: ${update.filesFound}\nSupport chunks: ${storage.load().size}\nIndex: ${state.resolve("index.json")}") }
        catch (error: Exception) { System.err.println("Support index update failed: ${error.message}"); exitProcess(1) }
        return
    }
    val stateDir = config.projectRoot.resolve(".developer-assistant")
    val indexPath = stateDir.resolve("index.json")
    val manifestPath = stateDir.resolve("manifest.json")
    val indexStorage = IndexStorage(indexPath)
    val manifestStorage = ManifestStorage(manifestPath)
    val git = GitMcpClient(config.mcpUrl)
    val llm = OpenAiResponsesClient(OpenAiConfig(config.openAiBaseUrl, config.openAiModel, config.openAiApiKey))

    println("Developer Assistant\n")
    println("Project: ${config.projectRoot}")
    if (reviewMode) {
        try {
            val request = ReviewCommandParser.parse(args, config.projectRoot)
            val embedding by lazy {
                OllamaEmbeddingClient(OllamaEmbeddingConfig(config.embeddingBaseUrl, config.embeddingModel))
            }
            val indexer by lazy {
                ProjectIndexer(
                    config.projectRoot,
                    ProjectScanner(ScannerConfig(maxFileSizeBytes = config.maxFileSizeBytes)),
                    ProjectChunker(config.chunkSize, config.chunkOverlap),
                    embedding,
                    "ollama",
                    config.embeddingModel,
                    indexStorage,
                    manifestStorage
                )
            }
            val retriever by lazy { ProjectRetriever(embedding, config.topK) }
            val ragContextProvider = ReviewRagContextProvider(
                mode = request.ragMode,
                indexExists = indexStorage::exists,
                updateIndex = {
                    if (!indexStorage.exists()) println("Index not found.\nBuilding project index...")
                    else println("Scanning project...")
                    printUpdate(indexer.update(progress = ::println))
                },
                search = { query -> retriever.retrieve(query, indexStorage.load()) }
            )
            val service = PullRequestReviewService(git, ragContextProvider, llm)
            runBlocking { service.execute(request) }
            println("AI review written to ${request.output.toAbsolutePath().normalize()}")
            return
        } catch (error: Exception) {
            System.err.println("PR review failed: ${error.message}")
            if (config.debug) error.printStackTrace()
            exitProcess(1)
        }
    }

    val embedding = OllamaEmbeddingClient(OllamaEmbeddingConfig(config.embeddingBaseUrl, config.embeddingModel))
    val indexer = ProjectIndexer(
        config.projectRoot,
        ProjectScanner(ScannerConfig(maxFileSizeBytes = config.maxFileSizeBytes)),
        ProjectChunker(config.chunkSize, config.chunkOverlap), embedding, "ollama", config.embeddingModel, indexStorage, manifestStorage
    )
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

    val service = DeveloperAssistantService(config.projectRoot, indexStorage, ProjectRetriever(embedding, config.topK), git, llm)
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
