package com.aiassistant.rag

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class IndexStorage(private val path: Path) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    fun exists(): Boolean = Files.isRegularFile(path)
    fun loadIndex(): LocalVectorIndex = if (!Files.exists(path)) LocalVectorIndex() else Files.newBufferedReader(path).use {
        val json = com.google.gson.JsonParser.parseReader(it)
        if (json.isJsonArray) LocalVectorIndex(chunks = gson.fromJson(json, object : TypeToken<List<ProjectChunk>>() {}.type))
        else gson.fromJson(json, LocalVectorIndex::class.java) ?: LocalVectorIndex()
    }
    fun load(): List<ProjectChunk> = loadIndex().chunks
    fun save(index: LocalVectorIndex) = writeAtomically(path) { gson.toJson(index, it) }
    fun delete() = Files.deleteIfExists(path)
}

class ManifestStorage(private val path: Path) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    fun load(): IndexManifest = if (!Files.exists(path)) IndexManifest() else Files.newBufferedReader(path).use {
        gson.fromJson(it, IndexManifest::class.java) ?: IndexManifest()
    }
    fun save(manifest: IndexManifest) = writeAtomically(path) { gson.toJson(manifest, it) }
    fun delete() = Files.deleteIfExists(path)
}

private fun writeAtomically(path: Path, write: (java.io.Writer) -> Unit) {
    val directory = path.toAbsolutePath().parent
    Files.createDirectories(directory)
    val temp = Files.createTempFile(directory, ".${path.fileName}-", ".tmp")
    try {
        Files.newBufferedWriter(temp).use(write)
        runCatching {
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(temp)
    }
}