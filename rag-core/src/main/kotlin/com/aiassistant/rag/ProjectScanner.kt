package com.aiassistant.rag

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Logger

data class ScannerConfig(
    val extensions: Set<String> = setOf("md", "txt", "kt", "kts", "java", "gradle", "toml", "xml", "json", "yaml", "yml", "js", "ts"),
    val excludedDirectories: Set<String> = setOf(".git", ".gradle", ".idea", ".developer-assistant", "build", "out", "node_modules", "dist", "generated", "captures", ".externalNativeBuild", ".cxx", "secrets"),
    val excludedFileNames: Set<String> = setOf("local.properties", "secrets.properties", ".env"),
    val excludedExtensions: Set<String> = setOf("jks", "keystore", "apk", "aab", "jar", "class", "png", "jpg", "jpeg", "gif", "webp", "mp4", "zip"),
    val maxFileSizeBytes: Long = 1_048_576
)

class ProjectScanner(private val config: ScannerConfig = ScannerConfig()) {
    private val logger = Logger.getLogger(ProjectScanner::class.java.name)

    fun scan(root: Path): List<ProjectFile> {
        val normalizedRoot = root.toAbsolutePath().normalize()
        require(Files.isDirectory(normalizedRoot)) { "Project directory does not exist: $normalizedRoot" }
        val result = mutableListOf<ProjectFile>()
        Files.walkFileTree(normalizedRoot, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
                if (dir != normalizedRoot && dir.fileName.toString() in config.excludedDirectories) FileVisitResult.SKIP_SUBTREE
                else FileVisitResult.CONTINUE

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                try {
                    val name = file.fileName.toString()
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (attrs.isRegularFile && attrs.size() <= config.maxFileSizeBytes &&
                        name !in config.excludedFileNames && extension !in config.excludedExtensions &&
                        extension in config.extensions && !looksBinary(file)
                    ) {
                        result += ProjectFile(file, normalizedRoot.relativize(file).toString().replace('\\', '/'), extension, attrs.size(), attrs.lastModifiedTime().toMillis())
                    }
                } catch (error: Exception) {
                    logger.warning("Skipping unreadable file ${normalizedRoot.relativize(file)}: ${error.message}")
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult {
                logger.warning("Skipping unreadable path ${runCatching { normalizedRoot.relativize(file) }.getOrDefault(file)}: ${exc.message}")
                return FileVisitResult.CONTINUE
            }
        })
        return result.sortedBy { it.relativePath }
    }

    private fun looksBinary(file: Path): Boolean = Files.newInputStream(file).use { input ->
        val bytes = ByteArray(4096)
        val count = input.read(bytes)
        count > 0 && bytes.take(count).any { it == 0.toByte() }
    }
}
