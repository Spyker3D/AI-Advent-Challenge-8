package com.aiassistant.rag

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageTest {
    @Test
    fun `index storage round trips metadata chunks and deletion`() {
        val path = Files.createTempDirectory("index-storage").resolve("nested/index.json")
        val storage = IndexStorage(path)
        val index = LocalVectorIndex(
            metadata = IndexMetadata("local", "test-model", 2),
            chunks = listOf(chunk())
        )

        assertFalse(storage.exists())
        storage.save(index)

        assertTrue(storage.exists())
        assertEquals(index, storage.loadIndex())
        assertEquals(index.chunks, storage.load())
        assertTrue(storage.delete())
        assertFalse(storage.exists())
        assertFalse(storage.delete())
    }

    @Test
    fun `index storage loads legacy chunk array`() {
        val path = Files.createTempDirectory("legacy-index").resolve("index.json")
        path.writeText(
            """
            [
              {
                "id": "legacy",
                "sourcePath": "README.md",
                "content": "legacy content",
                "fileExtension": "md",
                "startLine": 1,
                "endLine": 1,
                "contentHash": "hash",
                "embedding": [1.0]
              }
            ]
            """.trimIndent()
        )

        val loaded = IndexStorage(path).loadIndex()

        assertEquals(null, loaded.metadata)
        assertEquals("legacy", loaded.chunks.single().id)
        assertEquals(listOf(1f), loaded.chunks.single().embedding)
    }

    @Test
    fun `missing storage returns empty defaults`() {
        val root = Files.createTempDirectory("missing-storage")

        assertEquals(LocalVectorIndex(), IndexStorage(root.resolve("index.json")).loadIndex())
        assertEquals(IndexManifest(), ManifestStorage(root.resolve("manifest.json")).load())
    }

    @Test
    fun `manifest storage round trips entries and deletion`() {
        val path = Files.createTempDirectory("manifest-storage").resolve("nested/manifest.json")
        val storage = ManifestStorage(path)
        val manifest = IndexManifest(
            files = mapOf("A.kt" to ManifestEntry("sha", 42, listOf("chunk-1"))),
            lastIndexedEpochMillis = 99
        )

        storage.save(manifest)

        assertEquals(manifest, storage.load())
        assertTrue(storage.delete())
        assertEquals(IndexManifest(), storage.load())
    }

    @Test
    fun `malformed JSON is reported to caller`() {
        val root = Files.createTempDirectory("malformed-storage")
        val indexPath = root.resolve("index.json").also { it.writeText("{") }
        val manifestPath = root.resolve("manifest.json").also { it.writeText("{") }

        assertFails { IndexStorage(indexPath).loadIndex() }
        assertFails { ManifestStorage(manifestPath).load() }
    }

    private fun chunk() = ProjectChunk(
        id = "chunk-1",
        sourcePath = "src/A.kt",
        content = "class A",
        fileExtension = "kt",
        startLine = 1,
        endLine = 1,
        symbolName = "A",
        contentHash = "hash",
        embedding = listOf(1f, 2f)
    )
}
