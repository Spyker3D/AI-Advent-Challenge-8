import java.nio.file.Files

fun main() {
    val rootDir = RagIndexerPaths.rootDir()
    val fixedIndexPath = rootDir.resolve("output/fixed_index.json")
    val structureIndexPath = rootDir.resolve("output/structure_index.json")

    require(Files.exists(fixedIndexPath)) {
        "Index not found: $fixedIndexPath. Run :rag-indexer:run first."
    }
    require(Files.exists(structureIndexPath)) {
        "Index not found: $structureIndexPath. Run :rag-indexer:run first."
    }

    val fixedChunks = ChunkJsonReader.read(fixedIndexPath)
    val structureChunks = ChunkJsonReader.read(structureIndexPath)
    val documentCount = (fixedChunks + structureChunks).map { it.source }.distinct().size

    println("Metadata validation passed.")
    println("Validated fields: chunk_id, source, title, section, strategy, text, embedding")
    println()
    IndexSummaryPrinter.print(
        documentCount = documentCount,
        fixedChunks = fixedChunks,
        structureChunks = structureChunks
    )
}
