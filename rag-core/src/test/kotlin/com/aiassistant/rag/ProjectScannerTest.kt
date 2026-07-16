package com.aiassistant.rag

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectScannerTest {
    @Test fun `finds supported files and returns relative paths`() {
        val root = Files.createTempDirectory("scanner-test")
        root.resolve("docs").createDirectories().resolve("guide.md").writeText("guide")
        root.resolve("src/main").createDirectories().resolve("Main.kt").writeText("fun main() = Unit")
        val paths = ProjectScanner().scan(root).map { it.relativePath }
        assertEquals(listOf("docs/guide.md", "src/main/Main.kt"), paths)
    }

    @Test fun `ignores build git secrets and binary files`() {
        val root = Files.createTempDirectory("scanner-filter-test")
        root.resolve("module/build").createDirectories().resolve("Generated.kt").writeText("secret")
        root.resolve(".git").createDirectories().resolve("config.txt").writeText("secret")
        root.resolve("local.properties").writeText("api.key=secret")
        root.resolve("image.png").writeText("not really an image")
        root.resolve("README.md").writeText("safe")
        val paths = ProjectScanner().scan(root).map { it.relativePath }
        assertEquals(listOf("README.md"), paths)
        assertFalse(paths.any { "build" in it || ".git" in it })
    }

    @Test fun `skips a file that disappears during scan without failing project`() {
        val root = Files.createTempDirectory("scanner-safe-test")
        root.resolve("README.md").writeText("safe")
        assertTrue(ProjectScanner().scan(root).isNotEmpty())
    }
}
