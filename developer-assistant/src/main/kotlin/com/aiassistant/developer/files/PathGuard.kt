package com.aiassistant.developer.files

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

class PathGuard(root: Path) {
    val root: Path = root.toAbsolutePath().normalize()

    fun resolve(requested: String, write: Boolean = false): Path {
        require(requested.isNotBlank()) { "Path must not be empty." }
        val raw = Path.of(requested)
        val resolved = (if (raw.isAbsolute) raw else root.resolve(raw)).normalize().toAbsolutePath()
        require(resolved.startsWith(root)) { "Path is outside project root: $requested" }
        var cursor: Path? = if (write && !Files.exists(resolved)) resolved.parent else resolved
        while (cursor != null && cursor.startsWith(root)) {
            if (Files.isSymbolicLink(cursor)) {
                val real = cursor.toRealPath(LinkOption.NOFOLLOW_LINKS).parent.resolve(Files.readSymbolicLink(cursor)).normalize()
                require(real.startsWith(root)) { "Symbolic link points outside project root: $requested" }
            }
            if (cursor == root) break
            cursor = cursor.parent
        }
        return resolved
    }
}
