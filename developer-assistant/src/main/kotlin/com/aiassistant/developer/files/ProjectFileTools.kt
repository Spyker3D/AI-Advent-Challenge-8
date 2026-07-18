package com.aiassistant.developer.files

import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ProjectFileTools(
    projectRoot: Path,
    private val maxFileSize: Long = 1_048_576,
    private val maxResults: Int = 500
) {
    private val guard = PathGuard(projectRoot)
    private val staged = linkedMapOf<String, ProposedChange>()
    private val excludedDirs = setOf(".git", ".gradle", ".idea", ".developer-assistant", "build", "node_modules", "dist", "out", "coverage")
    private val allowedExtensions = setOf("kt", "kts", "java", "md", "txt", "json", "xml", "toml", "gradle", "js", "ts", "yaml", "yml", "properties")

    fun listFiles(path: String = ".", maxDepth: Int = 3, limit: Int = maxResults): List<String> {
        val base = guard.resolve(path)
        require(Files.isDirectory(base)) { "Directory not found: $path" }
        return Files.walk(base, maxDepth.coerceIn(1, 20)).use { stream ->
            stream.filter { Files.isRegularFile(it) && isAllowed(it) }
                .map { relative(it) }.sorted().limit(limit.coerceAtMost(maxResults).toLong()).toList()
        }
    }

    fun searchInFiles(query: String, path: String = ".", extensions: Set<String> = emptySet(), regex: Boolean = false, limit: Int = maxResults): List<SearchMatch> {
        require(query.isNotEmpty()) { "Search query must not be empty." }
        val matcher = if (regex) Regex(query) else null
        val result = mutableListOf<SearchMatch>()
        for (file in listFiles(path, 20, maxResults * 10)) {
            if (extensions.isNotEmpty() && ".${file.substringAfterLast('.', "")}" !in extensions) continue
            val lines = try { readRaw(file).lines() } catch (_: Exception) { continue }
            lines.forEachIndexed { index, line ->
                if (result.size < limit && (matcher?.containsMatchIn(line) ?: line.contains(query))) {
                    result += SearchMatch(file, index + 1, line.trim().take(300))
                }
            }
            if (result.size >= limit) break
        }
        return result
    }

    fun readFile(path: String, startLine: Int = 1, endLine: Int = 400): String {
        require(startLine >= 1 && endLine >= startLine) { "Invalid line range: $startLine..$endLine" }
        return readRaw(path).lines().drop(startLine - 1).take(endLine - startLine + 1)
            .mapIndexed { index, line -> "${startLine + index}: $line" }.joinToString("\n")
    }

    fun proposeWrite(path: String, content: String) {
        val target = guard.resolve(path, write = true)
        require(!isSecret(target)) { "Access to secret file is forbidden: $path" }
        val original = if (Files.exists(target)) readRaw(path) else null
        if (original == content) staged.remove(relative(target)) else staged[relative(target)] = ProposedChange(relative(target), original, content)
    }

    fun proposePatch(path: String, expected: String, replacement: String) {
        val current = staged[normalize(path)]?.content ?: readRaw(path)
        val occurrences = current.windowed(expected.length.coerceAtLeast(1)).count { it == expected }
        if (expected.isEmpty() || occurrences != 1) throw PatchConflict("Patch conflict in $path: expected text must occur exactly once (found $occurrences).")
        proposeWrite(path, current.replaceFirst(expected, replacement))
    }

    fun getDiff(): String = staged.values.joinToString("\n") { change -> UnifiedDiff.create(change.path, change.original, change.content) }.ifBlank { "No proposed changes." }
    fun proposedChanges(): List<ProposedChange> = staged.values.toList()
    fun clear() = staged.clear()

    fun applyConfirmed(confirmed: Boolean, dryRun: Boolean): List<String> {
        if (!confirmed || dryRun) return emptyList()
        val temporary = mutableListOf<Pair<Path, Path>>()
        try {
            staged.values.forEach { change ->
                val target = guard.resolve(change.path, write = true)
                Files.createDirectories(target.parent)
                val temp = Files.createTempFile(target.parent, ".day34-", ".tmp")
                Files.writeString(temp, change.content, StandardCharsets.UTF_8)
                temporary += temp to target
            }
            temporary.forEach { (temp, target) ->
                runCatching { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
                    .getOrElse { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING) }
            }
            return staged.keys.toList().also { staged.clear() }
        } finally {
            temporary.forEach { Files.deleteIfExists(it.first) }
        }
    }

    private fun readRaw(path: String): String {
        val file = guard.resolve(path)
        require(Files.isRegularFile(file)) { "File not found: $path" }
        require(!isSecret(file)) { "Access to secret file is forbidden: $path" }
        require(Files.size(file) <= maxFileSize) { "File is too large: $path" }
        val bytes = Files.readAllBytes(file)
        require(bytes.take(4096).none { it == 0.toByte() }) { "Binary file is not supported: $path" }
        return try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (error: Exception) { throw IllegalArgumentException("File is not valid UTF-8: $path") }
    }

    private fun isAllowed(path: Path): Boolean = path.none { it.toString() in excludedDirs } &&
        !isSecret(path) && path.fileName.toString().substringAfterLast('.', "").lowercase() in allowedExtensions
    private fun isSecret(path: Path): Boolean {
        val name = path.fileName.toString().lowercase()
        return name == "local.properties" || name == ".env" || name.startsWith(".env.") || name.endsWith(".jks") ||
            name.endsWith(".keystore") || (name.startsWith("service-account") && name.endsWith(".json")) ||
            (name.startsWith("credentials") && name.endsWith(".json"))
    }
    private fun normalize(path: String) = relative(guard.resolve(path, write = true))
    private fun relative(path: Path) = guard.root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/')
}

object UnifiedDiff {
    fun create(path: String, original: String?, updated: String): String {
        val old = original?.lines().orEmpty()
        val next = updated.lines()
        if (old == next) return ""
        if (original == null) return buildString {
            appendLine("diff --git a/$path b/$path")
            appendLine("new file mode 100644")
            appendLine("--- /dev/null")
            appendLine("+++ b/$path")
            appendLine("@@ -0,0 +1,${next.size} @@")
            next.forEach { appendLine("+$it") }
        }.trimEnd()

        var prefix = 0
        while (prefix < old.size && prefix < next.size && old[prefix] == next[prefix]) prefix++
        var suffix = 0
        while (suffix < old.size - prefix && suffix < next.size - prefix && old[old.lastIndex - suffix] == next[next.lastIndex - suffix]) suffix++
        val contextStart = (prefix - 3).coerceAtLeast(0)
        val oldChangedEnd = old.size - suffix
        val newChangedEnd = next.size - suffix
        val oldContextEnd = (oldChangedEnd + 3).coerceAtMost(old.size)
        val newContextEnd = (newChangedEnd + 3).coerceAtMost(next.size)
        val oldCount = oldContextEnd - contextStart
        val newCount = newContextEnd - contextStart
        return buildString {
            appendLine("diff --git a/$path b/$path")
            appendLine("--- a/$path")
            appendLine("+++ b/$path")
            appendLine("@@ -${contextStart + 1},$oldCount +${contextStart + 1},$newCount @@")
            old.subList(contextStart, prefix).forEach { appendLine(" $it") }
            old.subList(prefix, oldChangedEnd).forEach { appendLine("-$it") }
            next.subList(prefix, newChangedEnd).forEach { appendLine("+$it") }
            old.subList(oldChangedEnd, oldContextEnd).forEach { appendLine(" $it") }
        }.trimEnd()
    }
}
