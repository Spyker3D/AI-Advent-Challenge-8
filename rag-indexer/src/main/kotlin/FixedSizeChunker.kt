class FixedSizeChunker(
    private val chunkSizeChars: Int = 1200
) : Chunker {
    override fun chunk(documents: List<LoadedDocument>): List<Chunk> {
        var nextId = 1
        return documents.flatMap { document ->
            splitText(document.text, chunkSizeChars).map { text ->
                Chunk(
                    chunkId = "fixed_${nextId++.toString().padStart(6, '0')}",
                    source = document.source,
                    title = document.title,
                    section = "fixed",
                    strategy = "fixed-size",
                    text = text
                )
            }
        }
    }

    private fun splitText(text: String, chunkSize: Int): List<String> {
        val normalized = text.trim()
        if (normalized.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < normalized.length) {
            val end = (start + chunkSize).coerceAtMost(normalized.length)
            chunks += normalized.substring(start, end).trim()
            start = end
        }
        return chunks.filter { it.isNotBlank() }
    }
}
