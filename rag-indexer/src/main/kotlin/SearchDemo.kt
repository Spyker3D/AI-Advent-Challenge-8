import java.nio.file.Files
import kotlin.math.sqrt

fun main(args: Array<String>) {
    val query = args.joinToString(" ").ifBlank { "rememberSaveable" }
    val rootDir = RagIndexerPaths.rootDir()
    val indexPath = rootDir.resolve("output/structure_index.json")

    require(Files.exists(indexPath)) {
        "Index not found: $indexPath. Run :rag-indexer:run first."
    }

    val chunks = ChunkJsonReader.read(indexPath).filter { !it.embedding.isNullOrEmpty() }
    require(chunks.isNotEmpty()) {
        "Index contains no chunks with embeddings: $indexPath"
    }

    val queryEmbedding = OllamaEmbeddingClient().embed(query)
    val results = chunks
        .map { chunk -> SearchResult(chunk = chunk, score = cosineSimilarity(queryEmbedding, chunk.embedding.orEmpty())) }
        .sortedByDescending { it.score }
        .take(5)

    println("------------------------------------------------")
    println()
    println("Query:")
    println(query)
    println()
    println("Top 5 chunks:")
    println()

    results.forEach { result ->
        println("Score: ${"%.4f".format(result.score)}")
        println()
        println("Source:")
        println(result.chunk.source)
        println()
        println("Section:")
        println(result.chunk.section ?: "null")
        println()
        println("Preview:")
        println(result.chunk.text.toPreview())
        println()
        println("------------------------------------------------")
        println()
    }
}

private data class SearchResult(
    val chunk: Chunk,
    val score: Double
)

private fun cosineSimilarity(left: List<Float>, right: List<Float>): Double {
    val size = minOf(left.size, right.size)
    if (size == 0) return 0.0

    var dot = 0.0
    var leftNorm = 0.0
    var rightNorm = 0.0

    for (index in 0 until size) {
        val a = left[index].toDouble()
        val b = right[index].toDouble()
        dot += a * b
        leftNorm += a * a
        rightNorm += b * b
    }

    if (leftNorm == 0.0 || rightNorm == 0.0) return 0.0
    return dot / (sqrt(leftNorm) * sqrt(rightNorm))
}

private fun String.toPreview(maxLength: Int = 220): String {
    val compact = replace(Regex("\\s+"), " ").trim()
    return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
}
