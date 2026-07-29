package com.aiassistant.rag

import java.nio.file.Files
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
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


    @Test
    fun `repeated saves replace complete index and manifest without temporary files`() {
        val root = Files.createTempDirectory("replacement-storage")
        val indexPath = root.resolve("index.json")
        val manifestPath = root.resolve("manifest.json")
        val indexStorage = IndexStorage(indexPath)
        val manifestStorage = ManifestStorage(manifestPath)

        indexStorage.save(LocalVectorIndex(IndexMetadata("local", "old-model", 2), listOf(chunk())))
        manifestStorage.save(IndexManifest(mapOf("A.kt" to ManifestEntry("old", 1, listOf("chunk-1"))), 1))
        indexStorage.save(LocalVectorIndex(IndexMetadata("local", "new-model", 1), emptyList()))
        manifestStorage.save(IndexManifest(mapOf("B.kt" to ManifestEntry("new", 2, emptyList())), 2))

        assertEquals("new-model", indexStorage.loadIndex().metadata?.embeddingModel)
        assertTrue(indexStorage.load().isEmpty())
        assertEquals(setOf("B.kt"), manifestStorage.load().files.keys)
        assertFalse(indexPath.readText().contains("old-model"))
        assertFalse(manifestPath.readText().contains("A.kt"))
        assertTrue(Files.list(root).use { files -> files.noneMatch { it.fileName.toString().endsWith(".tmp") } })
    }

    @Test
    fun `failed replacement cleans temporary file without creating partial JSON`() {
        val root = Files.createTempDirectory("failed-storage")
        val target = root.resolve("index.json")
        Files.createDirectory(target)
        target.resolve("keep.txt").writeText("existing")

        assertFails { IndexStorage(target).save(LocalVectorIndex(chunks = listOf(chunk()))) }

        assertTrue(target.isDirectory())
        assertTrue(Files.list(root).use { files -> files.noneMatch { it.fileName.toString().endsWith(".tmp") } })
    }    private fun chunk() = ProjectChunk(
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
