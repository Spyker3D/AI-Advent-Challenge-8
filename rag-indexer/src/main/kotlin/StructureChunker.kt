class StructureChunker(
    private val maxChunkSizeChars: Int = 1200,
    private val overlapChars: Int = 200
) : Chunker {
    override fun chunk(documents: List<LoadedDocument>): List<Chunk> {
        var nextId = 1
        val chunks = mutableListOf<Chunk>()

        documents.forEach { document ->
            val sections = when (document.extension) {
                "md" -> markdownSections(document)
                "kt", "kts" -> kotlinSections(document)
                else -> listOf(Section(section = null, text = document.text))
            }

            sections.forEach { section ->
                splitLargeSection(section.text, maxChunkSizeChars, overlapChars).forEach { part ->
                    chunks += Chunk(
                        chunkId = "structure_${nextId++.toString().padStart(6, '0')}",
                        source = document.source,
                        title = document.title,
                        section = section.section,
                        strategy = "structure-based",
                        text = part
                    )
                }
            }
        }

        return chunks
    }

    private fun markdownSections(document: LoadedDocument): List<Section> {
        val headingRegex = Regex("""^(#{1,3})\s+(.+?)\s*$""")
        val sections = mutableListOf<Section>()
        var currentTitle: String? = null
        val currentText = StringBuilder()

        document.text.lineSequence().forEach { line ->
            val heading = headingRegex.matchEntire(line)
            if (heading != null) {
                flushSection(sections, currentTitle, currentText)
                currentTitle = heading.groupValues[2].trim()
            } else {
                currentText.appendLine(line)
            }
        }
        flushSection(sections, currentTitle, currentText)

        return sections.ifEmpty { listOf(Section(section = document.title, text = document.text)) }
    }

    private fun kotlinSections(document: LoadedDocument): List<Section> {
        val declarationRegex = Regex("""^\s*(?:public|private|internal|protected|open|final|abstract|sealed|data|value|inline|suspend|tailrec|operator|infix|external|override|companion|expect|actual|\s)*\b(class|interface|object|fun)\s+([A-Za-z_][A-Za-z0-9_]*)""")
        val sections = mutableListOf<Section>()
        val lines = document.text.lines()
        val preamble = StringBuilder()

        var currentName: String? = null
        var currentText = StringBuilder()
        var braceDepth = 0
        var capturing = false

        fun startSection(name: String, line: String) {
            flushSection(sections, currentName, currentText)
            currentName = name
            currentText = StringBuilder().appendLine(line)
            braceDepth = line.count { it == '{' } - line.count { it == '}' }
            capturing = true
        }

        lines.forEach { line ->
            val declaration = declarationRegex.find(line)
            if (!capturing && declaration != null) {
                if (preamble.isNotBlank()) {
                    sections += Section(section = document.title, text = preamble.toString().trim())
                    preamble.clear()
                }
                startSection(declaration.groupValues[2], line)
            } else if (capturing) {
                val startsNestedDeclaration = braceDepth <= 0 && declaration != null
                if (startsNestedDeclaration) {
                    startSection(declaration!!.groupValues[2], line)
                } else {
                    currentText.appendLine(line)
                    braceDepth += line.count { it == '{' } - line.count { it == '}' }
                    if (braceDepth <= 0 && declaration == null && line.isBlank()) {
                        flushSection(sections, currentName, currentText)
                        currentName = null
                        currentText = StringBuilder()
                        capturing = false
                        braceDepth = 0
                    }
                }
            } else {
                preamble.appendLine(line)
            }
        }

        if (capturing) {
            flushSection(sections, currentName, currentText)
        } else if (preamble.isNotBlank()) {
            sections += Section(section = document.title, text = preamble.toString().trim())
        }

        return sections.ifEmpty { listOf(Section(section = document.title, text = document.text)) }
    }

    private fun splitLargeSection(text: String, maxSize: Int, overlap: Int): List<String> {
        val normalized = text.trim()
        if (normalized.isEmpty()) return emptyList()
        if (normalized.length <= maxSize) return listOf(normalized)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < normalized.length) {
            val end = (start + maxSize).coerceAtMost(normalized.length)
            chunks += normalized.substring(start, end).trim()
            if (end == normalized.length) break
            start = (end - overlap).coerceAtLeast(start + 1)
        }
        return chunks.filter { it.isNotBlank() }
    }

    private fun flushSection(sections: MutableList<Section>, section: String?, text: StringBuilder) {
        val trimmed = text.toString().trim()
        if (trimmed.isNotEmpty()) {
            sections += Section(section = section, text = trimmed)
            text.clear()
        }
    }

    private data class Section(
        val section: String?,
        val text: String
    )
}
