import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.math.sqrt
import kotlin.math.ln
import java.util.Locale

fun main(args: Array<String>) {
    val query = args.joinToString(" ").ifBlank { "rememberSaveable" }
    val rootDir = RagIndexerPaths.rootDir()
    val indexPath = rootDir.resolve("output/structure_index.json")
    val debugPath = rootDir.resolve("output/retrieval_debug.md")

    require(Files.exists(indexPath)) {
        "Index not found: $indexPath. Run :rag-indexer:run first."
    }

    val chunks = ChunkJsonReader.read(indexPath).filter { !it.embedding.isNullOrEmpty() }
    require(chunks.isNotEmpty()) {
        "Index contains no chunks with embeddings: $indexPath"
    }

    val queryEmbedding = OllamaEmbeddingClient().embed(query)
    val queryTerms = tokenize(query)
    val termWeights = questionTermWeights(queryTerms, chunks)
    val scoredChunks = chunks
        .map { chunk ->
            val cosineScore = cosineSimilarity(queryEmbedding, chunk.embedding.orEmpty())
            val keywordScore = keywordScore(termWeights, chunk)
            val metadataScore = metadataScore(termWeights, chunk)
            SearchResult(
                chunk = chunk,
                cosineScore = cosineScore,
                keywordScore = keywordScore,
                metadataScore = metadataScore,
                finalScore = finalScore(cosineScore, keywordScore, metadataScore)
            )
        }

    val cosineCandidates = scoredChunks
        .sortedByDescending { it.cosineScore }
        .take(20)

    val lexicalCandidates = scoredChunks
        .sortedWith(
            compareByDescending<SearchResult> { it.keywordScore + it.metadataScore }
                .thenByDescending { it.metadataScore }
                .thenByDescending { it.cosineScore }
        )
        .take(20)

    val candidates = (cosineCandidates + lexicalCandidates)
        .distinctBy { it.chunk.chunkId }

    val rerankedByFinalScore = candidates
        .sortedWith(
            compareByDescending<SearchResult> { it.finalScore }
                .thenByDescending { it.cosineScore }
                .thenBy { it.chunk.source }
                .thenBy { it.chunk.chunkId }
        )

    val results = selectPromptResults(rerankedByFinalScore, lexicalCandidates, promptTopK = 5)

    Files.createDirectories(debugPath.parent)
    debugPath.writeText(retrievalDebugReport(query, cosineCandidates, lexicalCandidates, results))

    println("------------------------------------------------")
    println()
    println("Query:")
    println(query)
    println()
    println("Top 5 chunks:")
    println()

    results.forEach { result ->
        println("Final score: ${result.finalScore.formatScore()}")
        println("Cosine score: ${result.cosineScore.formatScore()}")
        println("Keyword score: ${result.keywordScore.formatScore()}")
        println("Metadata score: ${result.metadataScore.formatScore()}")
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

    println("Wrote $debugPath")
}

private data class SearchResult(
    val chunk: Chunk,
    val cosineScore: Double,
    val keywordScore: Double,
    val metadataScore: Double,
    val finalScore: Double
)

private fun keywordScore(termWeights: Map<String, Double>, chunk: Chunk): Double {
    if (termWeights.isEmpty()) return 0.0
    val chunkTerms = tokenize("${chunk.title} ${chunk.section.orEmpty()} ${chunk.text}")
    if (chunkTerms.isEmpty()) return 0.0
    return weightedCoverage(termWeights) { term -> term in chunkTerms }
}

private fun metadataScore(termWeights: Map<String, Double>, chunk: Chunk): Double {
    if (termWeights.isEmpty()) return 0.0
    val titleTerms = tokenize(chunk.title)
    val sectionTerms = tokenize(chunk.section.orEmpty())
    val titleMatches = weightedCoverage(termWeights) { term -> term in titleTerms }
    val sectionMatches = weightedCoverage(termWeights) { term -> term in sectionTerms }
    return (titleMatches * 0.4 + sectionMatches * 0.6).coerceIn(0.0, 1.0)
}

private fun finalScore(cosineScore: Double, keywordScore: Double, metadataScore: Double): Double {
    return cosineScore * 0.75 + keywordScore * 0.20 + metadataScore * 0.05
}

private fun tokenize(text: String): Set<String> {
    return Regex("[\\p{L}\\p{N}]+")
        .findAll(text.lowercase(Locale.ROOT))
        .map { it.value }
        .filter { it.length >= 3 }
        .filterNot { it in STOP_WORDS }
        .toSet()
}

private fun questionTermWeights(questionTerms: Set<String>, chunks: List<Chunk>): Map<String, Double> {
    if (questionTerms.isEmpty() || chunks.isEmpty()) return emptyMap()

    val documentFrequency = mutableMapOf<String, Int>()
    chunks.forEach { chunk ->
        val chunkTerms = tokenize("${chunk.title} ${chunk.section.orEmpty()} ${chunk.text}")
        questionTerms.forEach { term ->
            if (term in chunkTerms) {
                documentFrequency[term] = documentFrequency.getOrDefault(term, 0) + 1
            }
        }
    }

    return questionTerms
        .mapNotNull { term ->
            val frequency = documentFrequency.getOrDefault(term, 0)
            if (frequency == 0) {
                null
            } else {
                term to ln((chunks.size + 1.0) / (frequency + 1.0)) + 1.0
            }
        }
        .toMap()
}

private fun weightedCoverage(
    termWeights: Map<String, Double>,
    matches: (String) -> Boolean
): Double {
    val totalWeight = termWeights.values.sum()
    if (totalWeight == 0.0) return 0.0
    val matchedWeight = termWeights.entries
        .filter { (term, _) -> matches(term) }
        .sumOf { (_, weight) -> weight }
    return (matchedWeight / totalWeight).coerceIn(0.0, 1.0)
}

private fun retrievalDebugReport(
    query: String,
    cosineCandidates: List<SearchResult>,
    lexicalCandidates: List<SearchResult>,
    results: List<SearchResult>
): String {
    return buildString {
        appendLine("# RAG Retrieval Debug")
        appendLine()
        appendLine("Question: $query")
        appendLine()
        appendLine("## Top 20 Before Rerank")
        appendLine()
        cosineCandidates.forEachIndexed { index, result ->
            appendResult(index + 1, result)
        }
        appendLine("## Top 20 Lexical/Metadata Candidates")
        appendLine()
        lexicalCandidates.forEachIndexed { index, result ->
            appendResult(index + 1, result)
        }
        appendLine("## Top 5 After Rerank")
        appendLine()
        results.forEachIndexed { index, result ->
            appendResult(index + 1, result)
        }
    }
}

private fun selectPromptResults(
    rerankedByFinalScore: List<SearchResult>,
    lexicalCandidates: List<SearchResult>,
    promptTopK: Int
): List<SearchResult> {
    val bestLexical = lexicalCandidates.firstOrNull {
        it.keywordScore > 0.0 || it.metadataScore > 0.0
    }
    val selected = mutableListOf<SearchResult>()
    if (bestLexical != null) {
        selected += bestLexical
    }
    rerankedByFinalScore.forEach { result ->
        if (selected.none { it.chunk.chunkId == result.chunk.chunkId }) {
            selected += result
        }
    }
    return selected.take(promptTopK)
}

private fun StringBuilder.appendResult(index: Int, result: SearchResult) {
    appendLine("### $index. ${result.chunk.source} / ${result.chunk.section ?: "N/A"}")
    appendLine()
    appendLine("- cosineScore: ${result.cosineScore.formatScore()}")
    appendLine("- keywordScore: ${result.keywordScore.formatScore()}")
    appendLine("- metadataScore: ${result.metadataScore.formatScore()}")
    appendLine("- finalScore: ${result.finalScore.formatScore()}")
    appendLine("- text preview: ${result.chunk.text.toPreview(300)}")
    appendLine()
}

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

private fun Double.formatScore(): String {
    return String.format(Locale.US, "%.4f", this)
}

private val STOP_WORDS = setOf(
    "как",
    "какой",
    "какая",
    "какие",
    "какое",
    "каком",
    "какую",
    "что",
    "где",
    "когда",
    "почему",
    "зачем",
    "чем",
    "или",
    "для",
    "про",
    "при",
    "над",
    "под",
    "это",
    "этот",
    "эта",
    "эти",
    "the",
    "and",
    "for",
    "with",
    "what",
    "which",
    "how",
    "why"
)
