package com.aiassistant.rag

import java.nio.file.Path

data class ProjectFile(
    val absolutePath: Path,
    val relativePath: String,
    val extension: String,
    val size: Long,
    val lastModified: Long
)

data class ProjectChunk(
    val id: String,
    val sourcePath: String,
    val content: String,
    val fileExtension: String,
    val startLine: Int?,
    val endLine: Int?,
    val symbolName: String?,
    val contentHash: String,
    val embedding: List<Float> = emptyList()
)

data class ManifestEntry(
    val sha256: String,
    val lastModified: Long,
    val chunkIds: List<String>
)

data class IndexManifest(
    val files: Map<String, ManifestEntry> = emptyMap(),
    val lastIndexedEpochMillis: Long = 0
)

data class ManifestDiff(
    val newFiles: Set<String>,
    val changedFiles: Set<String>,
    val deletedFiles: Set<String>,
    val unchangedFiles: Set<String>
)

data class IndexUpdate(
    val filesFound: Int,
    val newFiles: Int,
    val changedFiles: Int,
    val deletedFiles: Int,
    val unchangedFiles: Int,
    val generatedChunks: Int,
    val reusedChunks: Int
)

data class IndexMetadata(
    val embeddingProvider: String,
    val embeddingModel: String,
    val embeddingDimensions: Int
)

data class LocalVectorIndex(
    val metadata: IndexMetadata? = null,
    val chunks: List<ProjectChunk> = emptyList()
)
