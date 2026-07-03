object IndexSummaryPrinter {
    fun print(
        documentCount: Int,
        fixedChunks: List<Chunk>,
        structureChunks: List<Chunk>
    ) {
        val allChunks = fixedChunks + structureChunks
        val embeddingSize = allChunks.firstOrNull { !it.embedding.isNullOrEmpty() }?.embedding?.size ?: 0

        println("Index summary")
        println("Documents: $documentCount")
        println("Fixed chunks: ${fixedChunks.size}")
        println("Structure chunks: ${structureChunks.size}")
        println("Total chunks: ${allChunks.size}")
        println("Embedding size: $embeddingSize")
        println()
        println("First two chunks:")
        allChunks.take(2).forEach { chunk ->
            println()
            println("Chunk:")
            println("chunk_id: ${chunk.chunkId}")
            println("source: ${chunk.source}")
            println("section: ${chunk.section ?: "null"}")
            println("text preview: ${chunk.text.toPreview()}")
            println("embedding size: ${chunk.embedding?.size ?: 0}")
        }
    }

    private fun String.toPreview(maxLength: Int = 180): String {
        val compact = replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
    }
}
