package com.aiassistant.rag

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path

class IndexStorage(private val path: Path) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    fun loadIndex(): LocalVectorIndex = if (!Files.exists(path)) LocalVectorIndex() else Files.newBufferedReader(path).use {
        val json = com.google.gson.JsonParser.parseReader(it)
        if (json.isJsonArray) LocalVectorIndex(chunks = gson.fromJson(json, object : TypeToken<List<ProjectChunk>>() {}.type))
        else gson.fromJson(json, LocalVectorIndex::class.java) ?: LocalVectorIndex()
    }
    fun load(): List<ProjectChunk> = loadIndex().chunks
    fun save(index: LocalVectorIndex) { Files.createDirectories(path.parent); Files.newBufferedWriter(path).use { gson.toJson(index, it) } }
    fun delete() = Files.deleteIfExists(path)
}

class ManifestStorage(private val path: Path) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    fun load(): IndexManifest = if (!Files.exists(path)) IndexManifest() else Files.newBufferedReader(path).use {
        gson.fromJson(it, IndexManifest::class.java) ?: IndexManifest()
    }
    fun save(manifest: IndexManifest) { Files.createDirectories(path.parent); Files.newBufferedWriter(path).use { gson.toJson(manifest, it) } }
    fun delete() = Files.deleteIfExists(path)
}
