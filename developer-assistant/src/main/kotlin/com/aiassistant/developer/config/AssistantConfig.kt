package com.aiassistant.developer.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

data class AssistantConfig(
    val projectRoot: Path,
    val embeddingBaseUrl: String,
    val embeddingModel: String,
    val openAiBaseUrl: String,
    val openAiModel: String,
    val openAiApiKey: String,
    val mcpUrl: String,
    val topK: Int,
    val chunkSize: Int,
    val chunkOverlap: Int,
    val maxFileSizeBytes: Long,
    val dryRun: Boolean,
    val debug: Boolean
)

object ConfigLoader {
    fun load(args: Array<String>, env: Map<String, String> = System.getenv()): AssistantConfig {
        val options = args.filter { it.startsWith("--") }.associate {
            val pair = it.removePrefix("--").split('=', limit = 2)
            pair[0] to pair.getOrElse(1) { "true" }
        }
        val root = Paths.get(options["project-root"] ?: options["project"] ?: System.getProperty("user.dir"))
            .toAbsolutePath().normalize()
        require(Files.isDirectory(root)) { "Project directory does not exist: $root" }
        fun value(key: String, envKey: String, default: String) = options[key] ?: env[envKey] ?: default
        val apiKey = env["OPENAI_API_KEY"]?.takeIf { it.isNotBlank() }
            ?: loadOpenAiApiKey(root)
        require(!apiKey.isNullOrBlank()) {
            "OpenAI API key was not found. Set OPENAI_API_KEY in the environment or add OPENAI_API_KEY=... to ${root.resolve("local.properties")}."
        }
        return AssistantConfig(
            root,
            value("embedding-base-url", "OLLAMA_BASE_URL", "http://localhost:11434"),
            value("embedding-model", "OLLAMA_EMBEDDING_MODEL", "nomic-embed-text:latest"),
            value("openai-base-url", "OPENAI_BASE_URL", "https://api.openai.com/v1"),
            value("generation-model", "OPENAI_GENERATION_MODEL", "gpt-4.1-mini"),
            apiKey,
            value("mcp-url", "DEVELOPER_ASSISTANT_MCP_URL", "http://localhost:3000/mcp"),
            value("top-k", "DEVELOPER_ASSISTANT_TOP_K", "8").toInt(),
            value("chunk-size", "DEVELOPER_ASSISTANT_CHUNK_SIZE", "1200").toInt(),
            value("chunk-overlap", "DEVELOPER_ASSISTANT_CHUNK_OVERLAP", "200").toInt(),
            value("max-file-size", "DEVELOPER_ASSISTANT_MAX_FILE_SIZE", "1048576").toLong(),
            options["dry-run"]?.toBoolean() ?: false,
            options["debug"]?.toBoolean() ?: false
        )
    }

    private fun loadOpenAiApiKey(projectRoot: Path): String? {
        val path = projectRoot.resolve("local.properties")
        if (!Files.isRegularFile(path)) return null
        return runCatching {
            val properties = Properties()
            Files.newInputStream(path).use(properties::load)
            properties.getProperty("OPENAI_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrElse { error ->
            throw IllegalArgumentException("Cannot read OpenAI API key from $path: ${error.message}", error)
        }
    }
}
