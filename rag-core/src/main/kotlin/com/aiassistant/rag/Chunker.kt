package com.aiassistant.rag

import java.util.UUID

class ProjectChunker(private val chunkSize: Int = 1200, private val overlap: Int = 200) {
    init { require(chunkSize > 0 && overlap in 0 until chunkSize) }

    fun chunk(file: ProjectFile, text: String): List<ProjectChunk> {
        val lines = text.lines()
        if (lines.all { it.isBlank() }) return emptyList()
        val ranges = if (file.extension == "md") markdownRanges(lines) else lineRanges(lines)
        return ranges.map { range ->
            val content = lines.subList(range.first, range.last + 1).joinToString("\n").trim()
            val hash = FileHash.sha256(content)
            ProjectChunk(
                id = UUID.nameUUIDFromBytes("${file.relativePath}:${range.first + 1}:$hash".toByteArray()).toString(),
                sourcePath = file.relativePath,
                content = content,
                fileExtension = file.extension,
                startLine = range.first + 1,
                endLine = range.last + 1,
                symbolName = symbolName(lines[range.first]),
                contentHash = hash
            )
        }.filter { it.content.isNotBlank() }
    }

    private fun markdownRanges(lines: List<String>): List<IntRange> {
        val headings = lines.indices.filter { lines[it].matches(Regex("^#{1,6}\\s+.*")) }
        if (headings.isEmpty()) return lineRanges(lines)
        val boundaries = (listOf(0) + headings + listOf(lines.size)).distinct().sorted()
        return boundaries.zipWithNext().flatMap { (start, end) -> splitRange(lines, start, end) }
    }

    private fun lineRanges(lines: List<String>): List<IntRange> = splitRange(lines, 0, lines.size)

    private fun splitRange(lines: List<String>, from: Int, until: Int): List<IntRange> {
        if (from >= until) return emptyList()
        val result = mutableListOf<IntRange>()
        var start = from
        while (start < until) {
            var end = start
            var chars = 0
            while (end < until && (chars + lines[end].length + 1 <= chunkSize || end == start)) chars += lines[end++].length + 1
            result += start..(end - 1)
            if (end >= until) break
            var overlapChars = 0
            var next = end
            while (next > start + 1 && overlapChars < overlap) overlapChars += lines[--next].length + 1
            start = next.coerceAtLeast(start + 1)
        }
        return result
    }

    private fun symbolName(line: String): String? = Regex("\\b(?:class|interface|object|fun)\\s+([A-Za-z_][A-Za-z0-9_]*)")
        .find(line)?.groupValues?.get(1)
}
