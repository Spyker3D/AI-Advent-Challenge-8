package com.aiassistant.core.domain.rag

import org.junit.Assert.assertEquals
import org.junit.Test

class RagRetrieverTest {
    private val retriever = RagRetriever()

    @Test
    fun `search returns five results by default after rerank`() {
        val chunks = (1..12).map { index ->
            chunk(
                id = "chunk-$index",
                text = "General Android context $index",
                embedding = listOf(index.toFloat(), 0f)
            )
        }

        val results = retriever.search(
            question = "Android context",
            questionEmbedding = listOf(1f, 0f),
            chunks = chunks
        )

        assertEquals(5, results.size)
    }

    @Test
    fun `search reranks cosine candidates using keyword and metadata scores`() {
        val cosineOnly = chunk(
            id = "cosine-only",
            text = "General dependency injection notes",
            embedding = listOf(1f, 0f)
        )
        val lexicalMatch = chunk(
            id = "lexical-match",
            title = "RAG retrieval",
            section = "rerank",
            text = "Primary cosine search should rerank candidates after retrieval.",
            embedding = listOf(0.8f, 0.6f)
        )

        val results = retriever.search(
            question = "cosine retrieval rerank",
            questionEmbedding = listOf(1f, 0f),
            chunks = listOf(cosineOnly, lexicalMatch),
            config = RagRetrievalConfig.Improved.copy(
                candidateTopK = 2,
                finalTopK = 2,
                filteringEnabled = false
            )
        )

        assertEquals("lexical-match", results.first().chunk.chunkId)
        assertEquals(true, results.first().keywordScore > results.last().keywordScore)
        assertEquals(true, results.first().metadataScore > results.last().metadataScore)
    }

    @Test
    fun `baseline search sorts by cosine only`() {
        val cosineOnly = chunk(
            id = "cosine-only",
            text = "General dependency injection notes",
            embedding = listOf(1f, 0f)
        )
        val lexicalMatch = chunk(
            id = "lexical-match",
            title = "RAG retrieval",
            section = "rerank",
            text = "Primary cosine search should rerank candidates after retrieval.",
            embedding = listOf(0.8f, 0.6f)
        )

        val results = retriever.search(
            question = "cosine retrieval rerank",
            questionEmbedding = listOf(1f, 0f),
            chunks = listOf(cosineOnly, lexicalMatch),
            config = RagRetrievalConfig.Baseline.copy(
                candidateTopK = 2,
                finalTopK = 2
            )
        )

        assertEquals("cosine-only", results.first().chunk.chunkId)
        assertEquals(results.first().cosineScore, results.first().finalScore)
    }

    @Test
    fun `improved search uses original question terms for lexical rerank`() {
        val generalChatModule = chunk(
            id = "general-chat-module",
            title = "Chat Module",
            section = "Chat Module",
            text = "Документ описывает модуль чата Android AI Assistant.",
            embedding = listOf(1f, 0f)
        )
        val lifecycle = chunk(
            id = "message-lifecycle",
            title = "Chat Module",
            section = "5. Жизненный цикл сообщения",
            text = "Полный цикл: User types text, Send button, ViewModel, Repository, HTTP Request, LLM, Streaming Response, Compose UI.",
            embedding = listOf(0.7f, 0.3f)
        )

        val results = retriever.search(
            question = "message lifecycle Chat Module Android AI Assistant",
            lexicalQuestion = "Какой жизненный цикл сообщения описан в Chat Module Android AI Assistant? message lifecycle Chat Module Android AI Assistant",
            questionEmbedding = listOf(1f, 0f),
            chunks = listOf(generalChatModule, lifecycle),
            config = RagRetrievalConfig.Improved.copy(
                candidateTopK = 2,
                finalTopK = 2,
                filteringEnabled = false
            )
        )

        assertEquals("message-lifecycle", results.first().chunk.chunkId)
    }

    private fun chunk(
        id: String,
        text: String,
        embedding: List<Float>,
        title: String = "Title",
        section: String? = null
    ): RagChunk {
        return RagChunk(
            chunkId = id,
            source = "$id.md",
            title = title,
            section = section,
            strategy = "test",
            text = text,
            embedding = embedding
        )
    }
}
