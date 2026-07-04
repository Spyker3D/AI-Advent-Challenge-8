package com.aiassistant.core.domain.rag

import android.util.Log
import kotlin.math.sqrt
import kotlin.math.ln
import java.util.Locale
import javax.inject.Inject

class RagRetriever @Inject constructor() {
    fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        val size = minOf(a.size, b.size)
        if (size == 0) return 0f

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (index in 0 until size) {
            val left = a[index].toDouble()
            val right = b[index].toDouble()
            dot += left * right
            normA += left * left
            normB += right * right
        }

        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }

    fun search(
        question: String,
        questionEmbedding: List<Float>,
        chunks: List<RagChunk>,
        candidateTopK: Int = DEFAULT_CANDIDATE_TOP_K,
        promptTopK: Int = DEFAULT_PROMPT_TOP_K
    ): List<RagSearchResult> {
        val questionTerms = tokenize(question)
        val termWeights = questionTermWeights(questionTerms, chunks)

        val scoredChunks = chunks
            .asSequence()
            .filter { it.embedding.isNotEmpty() }
            .map { chunk ->
                val cosineScore = cosineSimilarity(questionEmbedding, chunk.embedding)
                val keywordScore = keywordScore(termWeights, chunk)
                val metadataScore = metadataScore(termWeights, chunk)
                RagSearchResult(
                    chunk = chunk,
                    cosineScore = cosineScore,
                    keywordScore = keywordScore,
                    metadataScore = metadataScore,
                    finalScore = finalScore(
                        cosineScore = cosineScore,
                        keywordScore = keywordScore,
                        metadataScore = metadataScore
                    )
                )
            }
            .toList()

        val cosineCandidates = scoredChunks
            .sortedByDescending { it.cosineScore }
            .take(candidateTopK)

        val lexicalCandidates = scoredChunks
            .sortedWith(
                compareByDescending<RagSearchResult> { it.keywordScore + it.metadataScore }
                    .thenByDescending { it.metadataScore }
                    .thenByDescending { it.cosineScore }
            )
            .take(candidateTopK)

        val candidates = (cosineCandidates + lexicalCandidates)
            .distinctBy { it.chunk.chunkId }

        val rerankedByFinalScore = candidates
            .sortedWith(
                compareByDescending<RagSearchResult> { it.finalScore }
                    .thenByDescending { it.cosineScore }
                    .thenBy { it.chunk.source }
                    .thenBy { it.chunk.chunkId }
            )

        val reranked = selectPromptResults(
            rerankedByFinalScore = rerankedByFinalScore,
            lexicalCandidates = lexicalCandidates,
            promptTopK = promptTopK
        )

        logRetrieval(question, cosineCandidates, lexicalCandidates, reranked)
        return reranked
    }

    private fun selectPromptResults(
        rerankedByFinalScore: List<RagSearchResult>,
        lexicalCandidates: List<RagSearchResult>,
        promptTopK: Int
    ): List<RagSearchResult> {
        val bestLexical = lexicalCandidates.firstOrNull {
            it.keywordScore > 0f || it.metadataScore > 0f
        }
        val selected = mutableListOf<RagSearchResult>()
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

    private fun keywordScore(termWeights: Map<String, Float>, chunk: RagChunk): Float {
        if (termWeights.isEmpty()) return 0f
        val chunkTerms = tokenize("${chunk.title} ${chunk.section.orEmpty()} ${chunk.text}")
        if (chunkTerms.isEmpty()) return 0f
        return weightedCoverage(termWeights) { term -> term in chunkTerms }
    }

    private fun metadataScore(termWeights: Map<String, Float>, chunk: RagChunk): Float {
        if (termWeights.isEmpty()) return 0f
        val titleTerms = tokenize(chunk.title)
        val sectionTerms = tokenize(chunk.section.orEmpty())
        val titleMatches = weightedCoverage(termWeights) { term -> term in titleTerms }
        val sectionMatches = weightedCoverage(termWeights) { term -> term in sectionTerms }
        return (titleMatches * TITLE_METADATA_WEIGHT + sectionMatches * SECTION_METADATA_WEIGHT)
            .coerceIn(0f, 1f)
    }

    private fun finalScore(
        cosineScore: Float,
        keywordScore: Float,
        metadataScore: Float
    ): Float {
        return cosineScore * COSINE_WEIGHT +
            keywordScore * KEYWORD_WEIGHT +
            metadataScore * METADATA_WEIGHT
    }

    private fun logRetrieval(
        question: String,
        cosineCandidates: List<RagSearchResult>,
        lexicalCandidates: List<RagSearchResult>,
        reranked: List<RagSearchResult>
    ) {
        val message = buildString {
            appendLine("question=$question")
            appendLine("top 20 before rerank")
            cosineCandidates.forEachIndexed { index, result ->
                appendResult(index + 1, result)
            }
            appendLine("top 20 lexical/metadata candidates")
            lexicalCandidates.forEachIndexed { index, result ->
                appendResult(index + 1, result)
            }
            appendLine("top 5 after rerank")
            reranked.forEachIndexed { index, result ->
                appendResult(index + 1, result)
            }
        }
        try {
            Log.d(LOG_TAG, message)
        } catch (_: RuntimeException) {
            println("$LOG_TAG:\n$message")
        }
    }

    private fun StringBuilder.appendResult(index: Int, result: RagSearchResult) {
        val chunk = result.chunk
        appendLine(
            "#$index source=${chunk.source}, section=${chunk.section.orEmpty()}, " +
                "cosineScore=${result.cosineScore.formatScore()}, " +
                "keywordScore=${result.keywordScore.formatScore()}, " +
                "metadataScore=${result.metadataScore.formatScore()}, " +
                "finalScore=${result.finalScore.formatScore()}"
        )
        appendLine("text preview=${chunk.text.preview(DEBUG_PREVIEW_CHARS)}")
    }

    private fun tokenize(text: String): Set<String> {
        return TOKEN_REGEX
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .filter { it.length >= MIN_TOKEN_LENGTH }
            .filterNot { it in STOP_WORDS }
            .toSet()
    }

    private fun questionTermWeights(
        questionTerms: Set<String>,
        chunks: List<RagChunk>
    ): Map<String, Float> {
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
                    val idf = ln((chunks.size + 1.0) / (frequency + 1.0)).toFloat() + 1f
                    term to idf
                }
            }
            .toMap()
    }

    private fun weightedCoverage(
        termWeights: Map<String, Float>,
        matches: (String) -> Boolean
    ): Float {
        val totalWeight = termWeights.values.sum()
        if (totalWeight == 0f) return 0f
        val matchedWeight = termWeights.entries
            .filter { (term, _) -> matches(term) }
            .sumOf { (_, weight) -> weight.toDouble() }
            .toFloat()
        return (matchedWeight / totalWeight).coerceIn(0f, 1f)
    }

    private fun Float.formatScore(): String {
        return String.format(Locale.US, "%.4f", this)
    }

    private fun String.preview(maxChars: Int): String {
        val compact = replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxChars) compact else compact.take(maxChars) + "..."
    }

    companion object {
        const val DEFAULT_CANDIDATE_TOP_K = 20
        const val DEFAULT_PROMPT_TOP_K = 5
        private const val LOG_TAG = "RAG_RETRIEVAL"
        private const val DEBUG_PREVIEW_CHARS = 300
        private const val MIN_TOKEN_LENGTH = 3
        private const val COSINE_WEIGHT = 0.75f
        private const val KEYWORD_WEIGHT = 0.20f
        private const val METADATA_WEIGHT = 0.05f
        private const val TITLE_METADATA_WEIGHT = 0.4f
        private const val SECTION_METADATA_WEIGHT = 0.6f
        private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")
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
    }
}
