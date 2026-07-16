package com.aiassistant.rag

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object FileHash {
    fun sha256(path: Path): String = sha256(Files.readAllBytes(path))
    fun sha256(content: String): String = sha256(content.toByteArray(Charsets.UTF_8))
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}

object ManifestDiffer {
    fun diff(previous: IndexManifest, currentHashes: Map<String, String>): ManifestDiff {
        val oldPaths = previous.files.keys
        val newPaths = currentHashes.keys
        return ManifestDiff(
            newFiles = newPaths - oldPaths,
            changedFiles = newPaths.intersect(oldPaths).filterTo(linkedSetOf()) { previous.files.getValue(it).sha256 != currentHashes.getValue(it) },
            deletedFiles = oldPaths - newPaths,
            unchangedFiles = newPaths.intersect(oldPaths).filterTo(linkedSetOf()) { previous.files.getValue(it).sha256 == currentHashes.getValue(it) }
        )
    }
}
