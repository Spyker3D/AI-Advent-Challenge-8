package com.aiassistant.rag

import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.sqrt

class ProjectIndexer(
    private val root: Path,
    private val scanner: ProjectScanner,
    private val chunker: ProjectChunker,
    private val embeddingClient: EmbeddingClient,
    private val embeddingProvider: String,
    private val embeddingModel: String,
    private val indexStorage: IndexStorage,
    private val manifestStorage: ManifestStorage
) {
    suspend fun update(force: Boolean = false, progress: (String) -> Unit = {}): IndexUpdate {
        if (force) { indexStorage.delete(); manifestStorage.delete() }
        val storedIndex = indexStorage.loadIndex()
        val modelChanged = storedIndex.metadata?.let {
            it.embeddingProvider != embeddingProvider || it.embeddingModel != embeddingModel
        } ?: storedIndex.chunks.isNotEmpty()
        if (modelChanged) {
            progress("Embedding model changed. Rebuilding the complete index...")
            indexStorage.delete()
            manifestStorage.delete()
        }
        val files = scanner.scan(root)
        val hashes = linkedMapOf<String, String>()
        files.forEach { file -> runCatching { FileHash.sha256(file.absolutePath) }.onSuccess { hashes[file.relativePath] = it }.onFailure { progress("Skipping ${file.relativePath}: ${it.message}") } }
        val previous = manifestStorage.load()
        val diff = ManifestDiffer.diff(previous, hashes)
        val oldChunks = if (modelChanged) emptyList() else storedIndex.chunks
        val keep = oldChunks.filter { it.sourcePath in diff.unchangedFiles }
        val changedPaths = diff.newFiles + diff.changedFiles
        val generated = mutableListOf<ProjectChunk>()
        files.filter { it.relativePath in changedPaths && it.relativePath in hashes }.forEach { file ->
            val text = try {
                Files.readString(file.absolutePath)
            } catch (error: Exception) {
                progress("Skipping unreadable file ${file.relativePath}: ${error.message}")
                hashes.remove(file.relativePath)
                return@forEach
            }
            try {
                val chunks = chunker.chunk(file, text)
                val vectors = embeddingClient.embed(chunks.map { it.content })
                generated += chunks.zip(vectors) { chunk, vector -> chunk.copy(embedding = vector) }
            } catch (error: Exception) {
                throw IllegalStateException("Failed to index ${file.relativePath}: ${error.message}", error)
            }
        }
        val allChunks = keep + generated
        val entries = files.filter { it.relativePath in hashes }.associate { file ->
            file.relativePath to ManifestEntry(hashes.getValue(file.relativePath), file.lastModified, allChunks.filter { it.sourcePath == file.relativePath }.map { it.id })
        }
        val dimensions = allChunks.firstOrNull()?.embedding?.size ?: storedIndex.metadata?.embeddingDimensions ?: 0
        check(allChunks.all { it.embedding.size == dimensions }) { "Embedding dimension mismatch in local index." }
        indexStorage.save(LocalVectorIndex(IndexMetadata(embeddingProvider, embeddingModel, dimensions), allChunks))
        manifestStorage.save(IndexManifest(entries, System.currentTimeMillis()))
        return IndexUpdate(files.size, diff.newFiles.size, diff.changedFiles.size, diff.deletedFiles.size, diff.unchangedFiles.size, generated.size, keep.size)
    }
}

class ProjectRetriever(private val embeddingClient: EmbeddingClient, private val topK: Int = 8) {
    suspend fun retrieve(question: String, chunks: List<ProjectChunk>): List<ProjectChunk> {
        val query = embeddingClient.embed(listOf(question)).single()
        return chunks.asSequence().filter { it.embedding.isNotEmpty() }.map { it to cosine(query, it.embedding) }
            .sortedByDescending { it.second }.take(topK).map { it.first }.toList()
    }

    private fun cosine(a: List<Float>, b: List<Float>): Double {
        if (a.size != b.size || a.isEmpty()) return 0.0
        var dot = 0.0; var an = 0.0; var bn = 0.0
        for (i in a.indices) { dot += a[i] * b[i]; an += a[i] * a[i]; bn += b[i] * b[i] }
        return if (an == 0.0 || bn == 0.0) 0.0 else dot / (sqrt(an) * sqrt(bn))
    }
}
