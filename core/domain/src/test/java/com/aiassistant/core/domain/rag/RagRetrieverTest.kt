package com.aiassistant.core.domain.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RagRetrieverTest {
    private val retriever = RagRetriever()
    private val promptBuilder = RagPromptBuilder(
        recentHistoryFormatter = RecentHistoryFormatter(),
        taskMemoryPromptFormatter = TaskMemoryPromptFormatter()
    )

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

    @Test
    fun `confidence is average of top three final scores`() {
        val results = listOf(
            result("one", 0.9f),
            result("two", 0.6f),
            result("three", 0.3f),
            result("four", 0.1f)
        )

        assertEquals(0.6f, retriever.confidence(results), 0.0001f)
    }

    @Test
    fun `confidence is zero for empty results`() {
        assertEquals(0f, retriever.confidence(emptyList()), 0.0001f)
    }

    @Test
    fun `prompt quote is limited`() {
        val chunk = chunk(
            id = "long",
            text = (1..100).joinToString(" ") { "Sentence $it." },
            embedding = listOf(1f, 0f)
        )

        assertEquals(true, chunk.toPromptQuote().length <= 800)
    }

    @Test
    fun `rag prompt asks model to include relevant quote facts in answer`() {
        val prompt = promptBuilder.build(
            question = "What role does Retrofit play?",
            results = listOf(
                result(
                    id = "retrofit",
                    finalScore = 0.8f,
                    text = "Retrofit sends user messages to OpenAI and receives model answers."
                )
            )
        )

        assertEquals(true, prompt.contains("Do not leave important facts only in Sources or Quotes."))
        assertEquals(true, prompt.contains("OpenAI"))
    }

    @Test
    fun `rag prompt includes task memory and recent conversation before retrieved context`() {
        val prompt = promptBuilder.build(
            question = "How should this be updated?",
            results = listOf(result("rag", 0.8f)),
            taskContext = com.aiassistant.core.domain.memory.TaskContext(
                id = "day25",
                title = "Day25 mini-chat",
                description = "Add RAG memory",
                goals = listOf("Run RAG every turn")
            ),
            recentMessages = listOf(
                com.aiassistant.core.domain.entity.Message(
                    id = "m1",
                    content = "Use existing Working Memory.",
                    role = com.aiassistant.core.domain.entity.MessageRole.USER
                )
            )
        )

        assertTrue(prompt.indexOf("Task Memory:") < prompt.indexOf("Retrieved RAG Context:"))
        assertTrue(prompt.contains("Day25 mini-chat"))
        assertTrue(prompt.contains("User: Use existing Working Memory."))
    }

    @Test
    fun `answer confidence threshold is calibrated for current hybrid score scale`() {
        assertEquals(0.45f, RagAnswerConfig().minimumConfidence, 0.0001f)
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

    private fun result(
        id: String,
        finalScore: Float,
        text: String = id
    ): RagSearchResult {
        return RagSearchResult(
            chunk = chunk(
                id = id,
                text = text,
                embedding = listOf(1f, 0f)
            ),
            finalScore = finalScore,
            cosineScore = finalScore,
            keywordScore = 0f,
            metadataScore = 0f
        )
    }
}
