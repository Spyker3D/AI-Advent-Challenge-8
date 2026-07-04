package com.aiassistant.core.domain.rag

interface RagIndexLoader {
    suspend fun loadChunks(): List<RagChunk>
}
