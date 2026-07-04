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
        config: RagRetrievalConfig = RagRetrievalConfig.Improved,
        lexicalQuestion: String = question
    ): List<RagSearchResult> {
        val questionTerms = tokenize(lexicalQuestion)
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
                        metadataScore = metadataScore,
                        config = config
                    )
                )
            }
            .toList()

        val cosineCandidates = scoredChunks
            .sortedByDescending { it.cosineScore }
            .take(config.candidateTopK)

        val lexicalCandidates = scoredChunks
            .sortedWith(
                compareByDescending<RagSearchResult> { it.keywordScore + it.metadataScore }
                    .thenByDescending { it.metadataScore }
                    .thenByDescending { it.cosineScore }
            )
            .take(config.candidateTopK)

        val candidatesBeforeFilter = cosineCandidates
        val filteredCandidates = if (config.filteringEnabled) {
            cosineCandidates.filter { it.cosineScore >= config.similarityThreshold }
        } else {
            cosineCandidates
        }
        val candidatesAfterFilter = if (config.filteringEnabled && filteredCandidates.size < MIN_FILTERED_RESULTS) {
            cosineCandidates.take(MIN_FILTERED_RESULTS)
        } else {
            filteredCandidates
        }
        val removedByThreshold = if (config.filteringEnabled) {
            cosineCandidates.filterNot { candidate ->
                candidatesAfterFilter.any { it.chunk.chunkId == candidate.chunk.chunkId }
            }
        } else {
            emptyList()
        }

        val allowedLexicalCandidates = lexicalCandidates.filter { lexicalCandidate ->
            lexicalCandidate.finalScore >= config.similarityThreshold ||
                lexicalCandidate.keywordScore >= LEXICAL_KEYWORD_BYPASS_THRESHOLD ||
                lexicalCandidate.metadataScore >= LEXICAL_METADATA_BYPASS_THRESHOLD
        }

        val candidates = (candidatesAfterFilter + allowedLexicalCandidates)
            .distinctBy { it.chunk.chunkId }

        val sortedCandidates = if (config.rerankingEnabled) {
            candidates.sortedWith(
                compareByDescending<RagSearchResult> { it.finalScore }
                    .thenByDescending { it.cosineScore }
                    .thenBy { it.chunk.source }
                    .thenBy { it.chunk.chunkId }
            )
        } else {
            candidates.map { result ->
                result.copy(finalScore = result.cosineScore)
            }.sortedWith(
                compareByDescending<RagSearchResult> { it.cosineScore }
                    .thenBy { it.chunk.source }
                    .thenBy { it.chunk.chunkId }
            )
        }

        val reranked = selectPromptResults(
            sortedCandidates = sortedCandidates,
            lexicalCandidates = lexicalCandidates,
            config = config
        )

        logRetrieval(
            question = question,
            lexicalQuestion = lexicalQuestion,
            config = config,
            candidatesBeforeFilter = candidatesBeforeFilter,
            candidatesAfterFilter = candidatesAfterFilter,
            removedByThreshold = removedByThreshold,
            lexicalCandidates = lexicalCandidates,
            reranked = reranked
        )
        return reranked
    }

    fun confidence(results: List<RagSearchResult>): Float {
        if (results.isEmpty()) return 0f
        return results
            .take(CONFIDENCE_TOP_K)
            .map { it.finalScore }
            .average()
            .toFloat()
    }

    private fun selectPromptResults(
        sortedCandidates: List<RagSearchResult>,
        lexicalCandidates: List<RagSearchResult>,
        config: RagRetrievalConfig
    ): List<RagSearchResult> {
        val bestLexical = if (config.rerankingEnabled) {
            lexicalCandidates.firstOrNull {
                it.finalScore >= config.similarityThreshold ||
                    it.keywordScore >= LEXICAL_KEYWORD_BYPASS_THRESHOLD ||
                    it.metadataScore >= LEXICAL_METADATA_BYPASS_THRESHOLD
            }
        } else {
            null
        }
        val selected = mutableListOf<RagSearchResult>()
        if (bestLexical != null) {
            selected += bestLexical
        }
        sortedCandidates.forEach { result ->
            if (selected.none { it.chunk.chunkId == result.chunk.chunkId }) {
                selected += result
            }
        }
        return selected.take(config.finalTopK)
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
        val sourceTerms = tokenize(chunk.source)
        val titleMatches = weightedCoverage(termWeights) { term -> term in titleTerms }
        val sectionMatches = weightedCoverage(termWeights) { term -> term in sectionTerms }
        val sourceMatches = weightedCoverage(termWeights) { term -> term in sourceTerms }
        return (titleMatches * TITLE_METADATA_WEIGHT +
            sectionMatches * SECTION_METADATA_WEIGHT +
            sourceMatches * SOURCE_METADATA_WEIGHT)
            .coerceIn(0f, 1f)
    }

    private fun finalScore(
        cosineScore: Float,
        keywordScore: Float,
        metadataScore: Float,
        config: RagRetrievalConfig
    ): Float {
        return if (config.rerankingEnabled) {
            cosineScore * config.cosineWeight +
                keywordScore * config.keywordWeight +
                metadataScore * config.metadataWeight
        } else {
            cosineScore
        }
    }

    private fun logRetrieval(
        question: String,
        lexicalQuestion: String,
        config: RagRetrievalConfig,
        candidatesBeforeFilter: List<RagSearchResult>,
        candidatesAfterFilter: List<RagSearchResult>,
        removedByThreshold: List<RagSearchResult>,
        lexicalCandidates: List<RagSearchResult>,
        reranked: List<RagSearchResult>
    ) {
        val message = buildString {
            appendLine("original question=$question")
            appendLine("lexical question=$lexicalQuestion")
            appendLine("candidateTopK=${config.candidateTopK}")
            appendLine("finalTopK=${config.finalTopK}")
            appendLine("similarityThreshold=${config.similarityThreshold.formatScore()}")
            appendLine("rewrite=${config.queryRewriteEnabled}, filter=${config.filteringEnabled}, rerank=${config.rerankingEnabled}")
            appendLine("candidates before filter=${candidatesBeforeFilter.size}")
            appendLine("candidates after filter=${candidatesAfterFilter.size}")
            appendLine("top candidates before filter")
            candidatesBeforeFilter.forEachIndexed { index, result ->
                appendResult(index + 1, result)
            }
            appendLine("removed by threshold=${removedByThreshold.size}")
            removedByThreshold.forEachIndexed { index, result ->
                appendResult(index + 1, result)
            }
            appendLine("top lexical/metadata candidates")
            lexicalCandidates.forEachIndexed { index, result ->
                appendResult(index + 1, result)
            }
            appendLine("top results after rerank")
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
        private const val LOG_TAG = "RAG_DAY23"
        private const val DEBUG_PREVIEW_CHARS = 300
        private const val MIN_TOKEN_LENGTH = 3
        private const val MIN_FILTERED_RESULTS = 3
        private const val CONFIDENCE_TOP_K = 3
        private const val LEXICAL_KEYWORD_BYPASS_THRESHOLD = 0.35f
        private const val LEXICAL_METADATA_BYPASS_THRESHOLD = 0.35f
        private const val TITLE_METADATA_WEIGHT = 0.35f
        private const val SECTION_METADATA_WEIGHT = 0.50f
        private const val SOURCE_METADATA_WEIGHT = 0.15f
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
