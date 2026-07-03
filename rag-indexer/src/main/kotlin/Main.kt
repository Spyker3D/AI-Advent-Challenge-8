fun main() {
    val rootDir = RagIndexerPaths.rootDir()
    val outputDir = rootDir.resolve("output")

    val documents = DocumentLoader(rootDir).load()
    println("Loaded ${documents.size} documents from ${rootDir.resolve("input")}")

    val fixedChunks = FixedSizeChunker().chunk(documents)
    val structureChunks = StructureChunker().chunk(documents)
    println("Generated ${fixedChunks.size} fixed-size chunks")
    println("Generated ${structureChunks.size} structure-based chunks")

    val embeddingClient = OllamaEmbeddingClient()
    val fixedWithEmbeddings = embedAll(fixedChunks, embeddingClient)
    val structureWithEmbeddings = embedAll(structureChunks, embeddingClient)

    val fixedIndexPath = outputDir.resolve("fixed_index.json")
    val structureIndexPath = outputDir.resolve("structure_index.json")
    val comparisonPath = outputDir.resolve("comparison.md")

    IndexWriter().write(fixedIndexPath, fixedWithEmbeddings)
    IndexWriter().write(structureIndexPath, structureWithEmbeddings)
    ComparisonReportWriter().write(comparisonPath, fixedWithEmbeddings, structureWithEmbeddings)

    ChunkJsonReader.validateMetadata(fixedIndexPath)
    ChunkJsonReader.validateMetadata(structureIndexPath)

    println("Wrote $fixedIndexPath")
    println("Wrote $structureIndexPath")
    println("Wrote $comparisonPath")
    println()
    IndexSummaryPrinter.print(
        documentCount = documents.size,
        fixedChunks = fixedWithEmbeddings,
        structureChunks = structureWithEmbeddings
    )
}

private fun embedAll(chunks: List<Chunk>, embeddingClient: OllamaEmbeddingClient): List<Chunk> {
    return chunks.mapIndexed { index, chunk ->
        println("Embedding ${index + 1}/${chunks.size}: ${chunk.chunkId}")
        chunk.copy(embedding = embeddingClient.embed(chunk.text))
    }
}
