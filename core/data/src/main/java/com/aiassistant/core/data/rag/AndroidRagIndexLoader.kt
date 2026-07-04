package com.aiassistant.core.data.rag

import android.content.Context
import com.aiassistant.core.domain.rag.RagChunk
import com.aiassistant.core.domain.rag.RagIndexLoader
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidRagIndexLoader @Inject constructor(
    private val context: Context,
    private val gson: Gson
) : RagIndexLoader {
    private val mutex = Mutex()
    private var cachedChunks: List<RagChunk>? = null

    override suspend fun loadChunks(): List<RagChunk> {
        cachedChunks?.let { return it }
        return mutex.withLock {
            cachedChunks ?: readChunks().also { cachedChunks = it }
        }
    }

    private suspend fun readChunks(): List<RagChunk> = withContext(Dispatchers.IO) {
        context.assets.open(INDEX_ASSET_PATH).bufferedReader().use { reader ->
            val type = object : TypeToken<List<RagChunkDto>>() {}.type
            val dtoList: List<RagChunkDto> = gson.fromJson(reader, type)
            dtoList.map { it.toDomain() }
        }
    }

    private data class RagChunkDto(
        @SerializedName("chunk_id") val chunkId: String,
        val source: String,
        val title: String,
        val section: String?,
        val strategy: String,
        val text: String,
        val embedding: List<Float>?
    ) {
        fun toDomain(): RagChunk {
            return RagChunk(
                chunkId = chunkId,
                source = source,
                title = title,
                section = section,
                strategy = strategy,
                text = text,
                embedding = embedding.orEmpty()
            )
        }
    }

    private companion object {
        const val INDEX_ASSET_PATH = "rag/structure_index.json"
    }
}
