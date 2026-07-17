package com.aiassistant.developer.review

import com.aiassistant.rag.ProjectChunk

class ReviewRagContextProvider(
    private val mode: RagMode,
    private val indexExists: () -> Boolean,
    private val updateIndex: suspend () -> Unit,
    private val search: suspend (String) -> List<ProjectChunk>,
    private val log: (String) -> Unit = ::println
) {
    private var existingIndexAvailable: Boolean? = null

    fun announce() {
        log("RAG mode: ${mode.name.lowercase()}")
        when (mode) {
            RagMode.OFF -> log("RAG disabled. Review will use PR metadata and git diff only.")
            RagMode.EXISTING -> {
                val available = indexExists()
                existingIndexAvailable = available
                log(if (available) "Existing RAG index loaded."
                    else "RAG index not found. Continuing without RAG context.")
            }
            RagMode.UPDATE -> Unit
        }
    }

    suspend fun retrieve(query: String): List<ProjectChunk> {
        return when (mode) {
            RagMode.OFF -> emptyList()
            RagMode.EXISTING -> {
                val available = existingIndexAvailable ?: indexExists()
                if (available) search(query) else emptyList()
            }
            RagMode.UPDATE -> {
                updateIndex()
                search(query)
            }
        }
    }
}
