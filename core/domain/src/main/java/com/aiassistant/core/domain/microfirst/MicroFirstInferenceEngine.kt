package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.inference.IncidentCategory
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MicroFirstInferenceEngine @Inject constructor(
    private val embeddingClient: EmbeddingClient,
    private val prototypeProvider: MicroPrototypeProvider,
    private val llmClient: LlmClient
) {
    private val cacheMutex = Mutex()
    @Volatile private var cachedCentroids: Map<IncidentCategory, List<Double>>? = null

    suspend fun execute(input: String): Result<MicroFirstResult> {
        val totalStart = nowNanos()
        val microStart = nowNanos()
        val classification = classify(input)
        val microLatency = elapsedMs(microStart)

        if (classification is Classification.Success && classification.value.status == MicroStatus.OK) {
            val category = classification.value.label
            val text = category?.let(MicroResponseFormatter::format)
            if (text != null) {
                return Result.success(
                    MicroFirstResult(text, true, false, classification.value, null, microLatency, null,
                        elapsedMs(totalStart), 0, null)
                )
            }
        }

        val reason = when (classification) {
            is Classification.Failure -> classification.reason
            is Classification.Success -> when {
                classification.value.score < MicroFirstConfig.MIN_TOP_SCORE -> MicroFallbackReason.LOW_SCORE
                classification.value.margin < MicroFirstConfig.MIN_MARGIN -> MicroFallbackReason.LOW_MARGIN
                else -> MicroFallbackReason.MICRO_RESULT_INVALID
            }
        }
        val microResult = (classification as? Classification.Success)?.value
        val fallbackStart = nowNanos()
        return fallback(input).map { fallback ->
            val fallbackLatency = elapsedMs(fallbackStart)
            MicroFirstResult(
                finalText = MicroResponseFormatter.format(fallback.response.title, fallback.response.message, fallback.response.userAction),
                handledByMicro = false,
                fallbackUsed = true,
                microResult = microResult,
                fallbackModel = MicroFirstConfig.FALLBACK_MODEL,
                microLatencyMs = microLatency,
                fallbackLatencyMs = fallbackLatency,
                totalLatencyMs = elapsedMs(totalStart),
                largeLlmCalls = fallback.calls,
                fallbackReason = reason
            )
        }
    }

    suspend fun clearCacheForTests() {
        cacheMutex.withLock { cachedCentroids = null }
    }

    private suspend fun classify(input: String): Classification {
        if (input.isBlank()) return Classification.Failure(MicroFallbackReason.MICRO_RESULT_INVALID)
        val centroids = when (val initialized = centroids()) {
            is Centroids.Success -> initialized.value
            is Centroids.Failure -> return Classification.Failure(initialized.reason)
        }
        val embeddingResult = callEmbedding(listOf(input))
        val embeddings = embeddingResult.getOrElse { return Classification.Failure(MicroFallbackReason.EMBEDDING_ERROR) }
        if (embeddings.size != 1) return Classification.Failure(MicroFallbackReason.INVALID_VECTOR)
        val query = VectorMath.normalize(embeddings.single())
            ?: return Classification.Failure(MicroFallbackReason.INVALID_VECTOR)
        val ranked = centroids.map { (label, centroid) ->
            val score = VectorMath.cosine(query, centroid)
                ?: return Classification.Failure(MicroFallbackReason.INVALID_VECTOR)
            MicroCandidate(label, score)
        }.sortedWith(compareByDescending<MicroCandidate> { it.score }.thenBy { it.label.ordinal })
        if (ranked.size < 2 || ranked.any { !it.score.isFinite() }) {
            return Classification.Failure(MicroFallbackReason.MICRO_RESULT_INVALID)
        }
        val score = ranked[0].score
        val margin = score - ranked[1].score
        if (!margin.isFinite()) return Classification.Failure(MicroFallbackReason.MICRO_RESULT_INVALID)
        val status = if (score >= MicroFirstConfig.MIN_TOP_SCORE && margin >= MicroFirstConfig.MIN_MARGIN) MicroStatus.OK else MicroStatus.UNSURE
        val label = ranked[0].label.takeIf { status == MicroStatus.OK }
        return Classification.Success(MicroClassificationResult(label, score, margin, status, ranked))
    }

    private suspend fun centroids(): Centroids {
        cachedCentroids?.let { return Centroids.Success(it) }
        return cacheMutex.withLock {
            cachedCentroids?.let { return@withLock Centroids.Success(it) }
            val prototypes = try {
                prototypeProvider.loadPrototypes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }.getOrElse { return@withLock Centroids.Failure(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR) }
            val categories = IncidentCategory.entries.filter { it != IncidentCategory.AMBIGUOUS }
            if (prototypes.keys != categories.toSet() || categories.any { prototypes[it].isNullOrEmpty() }) {
                return@withLock Centroids.Failure(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR)
            }
            val texts = categories.flatMap { prototypes.getValue(it) }
            val embedded = callEmbedding(texts).getOrElse {
                return@withLock Centroids.Failure(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR)
            }
            if (embedded.size != texts.size) return@withLock Centroids.Failure(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR)
            val normalized = embedded.map { VectorMath.normalize(it) ?: return@withLock Centroids.Failure(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR) }
            if (normalized.map { it.size }.distinct().size != 1) return@withLock Centroids.Failure(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR)
            var offset = 0
            val result = buildMap {
                categories.forEach { category ->
                    val count = prototypes.getValue(category).size
                    val centroid = VectorMath.centroid(normalized.subList(offset, offset + count))
                        ?: return@withLock Centroids.Failure(MicroFallbackReason.PROTOTYPE_INITIALIZATION_ERROR)
                    put(category, centroid)
                    offset += count
                }
            }
            cachedCentroids = result
            Centroids.Success(result)
        }
    }

    private suspend fun callEmbedding(texts: List<String>): Result<List<List<Float>>> = try {
        embeddingClient.embed(texts, MicroFirstConfig.MICRO_MODEL)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }.also { result ->
        val error = result.exceptionOrNull()
        if (error is CancellationException) throw error
    }

    private suspend fun fallback(input: String): Result<FallbackCall> {
        val first = callFallback(input, MicroFallbackContract.PROMPT)
        val firstResponse = first.getOrElse { return Result.failure(it) }
        return when (val parsed = MicroFallbackContract.parse(firstResponse.message)) {
            is FallbackParseResult.Success -> Result.success(FallbackCall(parsed.value, 1))
            is FallbackParseResult.Failure -> {
                if (!parsed.validationFailure) return Result.failure(IllegalArgumentException(parsed.error))
                val retry = callFallback(input, MicroFallbackContract.PROMPT + "\n\n" + MicroFallbackContract.CORRECTION)
                val retryResponse = retry.getOrElse { return Result.failure(it) }
                when (val corrected = MicroFallbackContract.parse(retryResponse.message)) {
                    is FallbackParseResult.Success -> Result.success(FallbackCall(corrected.value, 2))
                    is FallbackParseResult.Failure -> Result.failure(IllegalArgumentException(corrected.error))
                }
            }
        }
    }

    private suspend fun callFallback(input: String, systemPrompt: String) = try {
        llmClient.sendChat(
            messages = listOf(
                Message(UUID.randomUUID().toString(), systemPrompt, MessageRole.SYSTEM),
                Message(UUID.randomUUID().toString(), input, MessageRole.USER)
            ),
            maxTokens = FALLBACK_MAX_TOKENS,
            model = MicroFirstConfig.FALLBACK_MODEL,
            options = LlmRequestOptions(
                temperature = 0.0,
                numPredict = FALLBACK_MAX_TOKENS,
                stream = false,
                jsonSchema = MicroFallbackContract.SCHEMA,
                requiredProvider = AiProvider.LOCAL_OLLAMA
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }.also { result ->
        val error = result.exceptionOrNull()
        if (error is CancellationException) throw error
    }

    private fun nowNanos() = System.nanoTime()
    private fun elapsedMs(start: Long) = (System.nanoTime() - start) / 1_000_000

    private sealed class Classification {
        data class Success(val value: MicroClassificationResult) : Classification()
        data class Failure(val reason: MicroFallbackReason) : Classification()
    }
    private sealed class Centroids {
        data class Success(val value: Map<IncidentCategory, List<Double>>) : Centroids()
        data class Failure(val reason: MicroFallbackReason) : Centroids()
    }
    private data class FallbackCall(val response: FallbackResponse, val calls: Int)

    private companion object { const val FALLBACK_MAX_TOKENS = 180 }
}
