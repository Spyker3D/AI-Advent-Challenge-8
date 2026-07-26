package com.aiassistant.developer.review

import com.aiassistant.rag.ProjectChunk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewRagContextProviderTest {
    @Test
    fun `announce logs off mode without touching index`() {
        val logs = mutableListOf<String>()
        val provider = ReviewRagContextProvider(
            mode = RagMode.OFF,
            indexExists = { error("must not inspect index") },
            updateIndex = { error("must not update") },
            search = { error("must not search") },
            log = logs::add
        )

        provider.announce()

        assertEquals(
            listOf(
                "RAG mode: off",
                "RAG disabled. Review will use PR metadata and git diff only."
            ),
            logs
        )
    }

    @Test
    fun `existing mode caches announced availability across retrievals`() = runBlocking {
        var indexChecks = 0
        val queries = mutableListOf<String>()
        val provider = ReviewRagContextProvider(
            mode = RagMode.EXISTING,
            indexExists = { indexChecks++; true },
            updateIndex = { error("must not update") },
            search = { query -> queries += query; listOf(chunk(query)) },
            log = {}
        )

        provider.announce()
        val first = provider.retrieve("first")
        val second = provider.retrieve("second")

        assertEquals(1, indexChecks)
        assertEquals(listOf("first", "second"), queries)
        assertEquals(listOf("first"), first.map { it.content })
        assertEquals(listOf("second"), second.map { it.content })
    }

    @Test
    fun `announced missing index remains unavailable without update or search`() = runBlocking {
        var available = false
        var searched = false
        val logs = mutableListOf<String>()
        val provider = ReviewRagContextProvider(
            mode = RagMode.EXISTING,
            indexExists = { available },
            updateIndex = { error("must not update") },
            search = { searched = true; emptyList() },
            log = logs::add
        )

        provider.announce()
        available = true
        val result = provider.retrieve("review")

        assertTrue(result.isEmpty())
        assertFalse(searched)
        assertEquals(
            "RAG index not found. Continuing without RAG context.",
            logs.last()
        )
    }

    @Test
    fun `update mode updates before forwarding query to search`() = runBlocking {
        val operations = mutableListOf<String>()
        val provider = ReviewRagContextProvider(
            mode = RagMode.UPDATE,
            indexExists = { error("must not inspect index") },
            updateIndex = { operations += "update" },
            search = { query -> operations += "search:$query"; listOf(chunk("result")) },
            log = {}
        )

        val result = provider.retrieve("changed authentication")

        assertEquals(listOf("update", "search:changed authentication"), operations)
        assertEquals("result", result.single().content)
    }

    @Test
    fun `update failure propagates and prevents search`() {
        var searched = false
        val provider = ReviewRagContextProvider(
            mode = RagMode.UPDATE,
            indexExists = { false },
            updateIndex = { throw IllegalStateException("index failed") },
            search = { searched = true; emptyList() },
            log = {}
        )

        val error = assertFailsWith<IllegalStateException> {
            runBlocking { provider.retrieve("review") }
        }

        assertEquals("index failed", error.message)
        assertFalse(searched)
    }

    @Test
    fun `search failure propagates after successful update`() {
        var updated = false
        val provider = ReviewRagContextProvider(
            mode = RagMode.UPDATE,
            indexExists = { false },
            updateIndex = { updated = true },
            search = { throw IllegalArgumentException("search failed") },
            log = {}
        )

        val error = assertFailsWith<IllegalArgumentException> {
            runBlocking { provider.retrieve("review") }
        }

        assertTrue(updated)
        assertEquals("search failed", error.message)
    }

    private fun chunk(content: String) = ProjectChunk(
        id = content,
        sourcePath = "README.md",
        content = content,
        fileExtension = "md",
        startLine = 1,
        endLine = 1,
        symbolName = null,
        contentHash = "hash",
        embedding = listOf(1f)
    )
}
