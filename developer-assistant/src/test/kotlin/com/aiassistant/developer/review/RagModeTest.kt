package com.aiassistant.developer.review

import com.aiassistant.rag.ProjectChunk
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class RagModeTest {
    @Test fun `default mode is update`() {
        assertEquals(RagMode.UPDATE, request().ragMode)
    }

    @Test fun `CLI parses all modes`() {
        assertEquals(RagMode.OFF, request("off").ragMode)
        assertEquals(RagMode.EXISTING, request("existing").ragMode)
        assertEquals(RagMode.UPDATE, request("update").ragMode)
    }

    @Test fun `invalid mode lists supported values`() {
        val error = assertFailsWith<IllegalArgumentException> { request("test") }
        assertTrue(error.message.orEmpty().contains("Supported values: off, existing, update"))
    }

    @Test fun `off invokes neither index nor search`() = runBlocking {
        var indexChecked = false; var updated = false; var searched = false
        val provider = ReviewRagContextProvider(
            RagMode.OFF,
            { indexChecked = true; true },
            { updated = true },
            { searched = true; emptyList() },
            {}
        )
        assertTrue(provider.retrieve("review").isEmpty())
        assertFalse(indexChecked); assertFalse(updated); assertFalse(searched)
    }

    @Test fun `existing without index continues without build or search`() = runBlocking {
        var updated = false; var searched = false
        val provider = ReviewRagContextProvider(RagMode.EXISTING, { false }, { updated = true },
            { searched = true; emptyList() }, {})
        assertTrue(provider.retrieve("review").isEmpty())
        assertFalse(updated); assertFalse(searched)
    }

    @Test fun `existing index searches without update`() = runBlocking {
        var updated = false; var searched = false
        val provider = ReviewRagContextProvider(RagMode.EXISTING, { true }, { updated = true },
            { searched = true; listOf(chunk()) }, {})
        assertEquals(1, provider.retrieve("review").size)
        assertFalse(updated); assertTrue(searched)
    }

    @Test fun `update builds and searches`() = runBlocking {
        var updated = false; var searched = false
        val provider = ReviewRagContextProvider(RagMode.UPDATE, { false }, { updated = true },
            { searched = true; listOf(chunk()) }, {})
        assertEquals(1, provider.retrieve("review").size)
        assertTrue(updated); assertTrue(searched)
    }

    private fun request(mode: String? = null): ReviewRequest {
        val root = Files.createTempDirectory("rag-mode")
        val args = mutableListOf("review-pr", "--base-ref=base", "--head-ref=head", "--output=review.md")
        mode?.let { args += "--rag-mode=$it" }
        return ReviewCommandParser.parse(args.toTypedArray(), root)
    }

    private fun chunk() = ProjectChunk("id", "README.md", "context", "md", 1, 1, null, "hash", listOf(1f))
}
