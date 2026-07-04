package com.aiassistant.core.domain.rag

import kotlin.math.sqrt
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
        retrievalTopK: Int = DEFAULT_RETRIEVAL_TOP_K
    ): List<RagSearchResult> {
        val questionTerms = tokenize(question)

        return chunks
            .asSequence()
            .filter { it.embedding.isNotEmpty() }
            .map { chunk ->
                RagSearchResult(
                    chunk = chunk,
                    score = cosineSimilarity(questionEmbedding, chunk.embedding)
                )
            }
            .sortedByDescending { it.score }
            .take(retrievalTopK)
            .map { result ->
                result.copy(score = rerankScore(result, questionTerms))
            }
            .sortedWith(
                compareByDescending<RagSearchResult> { it.score }
                    .thenBy { it.chunk.source }
                    .thenBy { it.chunk.chunkId }
            )
            .toList()
    }

    private fun rerankScore(
        result: RagSearchResult,
        questionTerms: Set<String>
    ): Float {
        if (questionTerms.isEmpty()) return result.score

        val chunk = result.chunk
        val searchableText = buildString {
            append(chunk.title)
            append(' ')
            append(chunk.section.orEmpty())
            append(' ')
            append(chunk.text)
        }
        val chunkTerms = tokenize(searchableText)
        if (chunkTerms.isEmpty()) return result.score

        val overlap = questionTerms.count { it in chunkTerms }.toFloat() / questionTerms.size
        val normalizedCosine = ((result.score + 1f) / 2f).coerceIn(0f, 1f)
        return RERANK_COSINE_WEIGHT * normalizedCosine + RERANK_TERM_WEIGHT * overlap
    }

    private fun tokenize(text: String): Set<String> {
        return TOKEN_REGEX
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .filter { it.length >= MIN_TOKEN_LENGTH }
            .toSet()
    }

    companion object {
        const val DEFAULT_RETRIEVAL_TOP_K = 10
        private const val MIN_TOKEN_LENGTH = 3
        private const val RERANK_COSINE_WEIGHT = 0.75f
        private const val RERANK_TERM_WEIGHT = 0.25f
        private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")
    }
}
