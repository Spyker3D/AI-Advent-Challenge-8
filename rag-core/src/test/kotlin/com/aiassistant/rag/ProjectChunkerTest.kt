package com.aiassistant.rag

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ProjectChunkerTest {
    @Test
    fun `blank input produces no chunks`() {
        assertEquals(emptyList(), ProjectChunker().chunk(projectFile("notes.txt"), " \n\t"))
    }

    @Test
    fun `constructor rejects nonpositive size and invalid overlap`() {
        assertFailsWith<IllegalArgumentException> { ProjectChunker(chunkSize = 0) }
        assertFailsWith<IllegalArgumentException> { ProjectChunker(chunkSize = 10, overlap = -1) }
        assertFailsWith<IllegalArgumentException> { ProjectChunker(chunkSize = 10, overlap = 10) }
    }

    @Test
    fun `line chunks overlap and preserve line metadata`() {
        val chunks = ProjectChunker(chunkSize = 8, overlap = 4).chunk(
            projectFile("src/sample.kt"),
            "aaaa\nbbbb\ncccc"
        )

        assertEquals(listOf("aaaa", "bbbb", "cccc"), chunks.map { it.content })
        assertEquals(listOf(1, 2, 3), chunks.map { it.startLine })
        assertEquals(listOf(1, 2, 3), chunks.map { it.endLine })
    }

    @Test
    fun `markdown headings create sections with heading symbols absent`() {
        val chunks = ProjectChunker(chunkSize = 100, overlap = 0).chunk(
            projectFile("README.md"),
            "intro\n# First\nbody\n## Second\nmore"
        )

        assertEquals(listOf("intro", "# First\nbody", "## Second\nmore"), chunks.map { it.content })
        assertEquals(listOf(1, 2, 4), chunks.map { it.startLine })
        assertEquals(listOf(1, 3, 5), chunks.map { it.endLine })
        chunks.forEach { assertNull(it.symbolName) }
    }

    @Test
    fun `chunk identity is stable for same source and changes with content`() {
        val file = projectFile("src/Service.kt")
        val chunker = ProjectChunker()

        val first = chunker.chunk(file, "class Service")[0]
        val repeated = chunker.chunk(file, "class Service")[0]
        val changed = chunker.chunk(file, "class UpdatedService")[0]

        assertEquals(first.id, repeated.id)
        assertEquals(first.contentHash, repeated.contentHash)
        assertEquals("Service", first.symbolName)
        assertNotEquals(first.id, changed.id)
        assertNotEquals(first.contentHash, changed.contentHash)
    }

    private fun projectFile(relativePath: String) = ProjectFile(
        absolutePath = Path.of(relativePath),
        relativePath = relativePath,
        extension = relativePath.substringAfterLast('.', ""),
        size = 0,
        lastModified = 0
    )
}
