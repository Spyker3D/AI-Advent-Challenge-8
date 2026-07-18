package com.aiassistant.core.data.support

import android.content.Context
import com.aiassistant.core.domain.rag.RagChunk
import com.aiassistant.core.domain.rag.RagRetriever
import com.aiassistant.core.domain.support.KnowledgeChunk
import com.aiassistant.core.domain.support.SupportKnowledgeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetSupportKnowledgeProvider @Inject constructor(private val context: Context, private val retriever: RagRetriever) : SupportKnowledgeProvider {
    override suspend fun search(query: String): Result<List<KnowledgeChunk>> = withContext(Dispatchers.IO) { runCatching {
        val names = context.assets.list("")?.filter { it.endsWith(".md") }.orEmpty()
        if (names.isEmpty()) error("RAG_INDEX_MISSING")
        val terms = tokens(query)
        val chunks = names.map { name ->
            val text = context.assets.open(name).bufferedReader().use { it.readText() }
            RagChunk(name, "support-knowledge/$name", name, null, "support-markdown", text, terms.map { term -> if (tokens(text).contains(term)) 1f else 0f })
        }
        val vector = terms.map { 1f }
        retriever.search(query, vector, chunks).filter { it.keywordScore > 0f || it.cosineScore > 0f }.take(4).map { KnowledgeChunk(it.chunk.text.take(1800), it.chunk.source, it.chunk.title) }
    } }
    private fun tokens(value: String) = Regex("[\\p{L}\\p{N}_]+", RegexOption.IGNORE_CASE).findAll(value.lowercase(Locale.ROOT)).map { it.value }.filter { it.length > 2 }.distinct().take(64).toList()
}
