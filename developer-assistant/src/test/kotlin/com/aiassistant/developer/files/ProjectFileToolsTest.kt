package com.aiassistant.developer.files

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.*

class ProjectFileToolsTest {
    private fun root() = Files.createTempDirectory("day34-tools")

    @Test fun `normalizes safe paths and rejects traversal`() {
        val root = root(); val guard = PathGuard(root)
        assertEquals(root.resolve("docs/a.md"), guard.resolve("docs/../docs/a.md"))
        assertFailsWith<IllegalArgumentException> { guard.resolve("../secret.txt") }
    }

    @Test fun `secret files cannot be read`() {
        val root = root(); root.resolve(".env").writeText("TOKEN=secret")
        assertFailsWith<IllegalArgumentException> { ProjectFileTools(root).readFile(".env") }
    }

    @Test fun `search returns path line and text and range read has numbers`() {
        val root = root(); root.resolve("Sample.kt").writeText("first\nval service = SampleService()\nthird")
        val tools = ProjectFileTools(root)
        val match = tools.searchInFiles("SampleService").single()
        assertEquals("Sample.kt", match.path); assertEquals(2, match.line)
        assertEquals("2: val service = SampleService()\n3: third", tools.readFile("Sample.kt", 2, 3))
    }

    @Test fun `binary and oversized files are rejected`() {
        val root = root(); root.resolve("binary.txt").writeBytes(byteArrayOf(1, 0, 2)); root.resolve("large.md").writeText("abcdef")
        assertFailsWith<IllegalArgumentException> { ProjectFileTools(root).readFile("binary.txt") }
        assertFailsWith<IllegalArgumentException> { ProjectFileTools(root, maxFileSize = 3).readFile("large.md") }
    }

    @Test fun `diff describes new file and declined write changes nothing`() {
        val root = root(); val tools = ProjectFileTools(root)
        tools.proposeWrite("docs/report.md", "hello")
        assertContains(tools.getDiff(), "--- /dev/null"); assertContains(tools.getDiff(), "+hello")
        assertTrue(tools.applyConfirmed(false, false).isEmpty())
        assertFalse(Files.exists(root.resolve("docs/report.md")))
    }

    @Test fun `diff for existing file contains only changed area and context`() {
        val root = root(); root.resolve("README.md").writeText((1..20).joinToString("\n") { "line $it" })
        val tools = ProjectFileTools(root); tools.proposePatch("README.md", "line 10", "changed 10")
        val diff = tools.getDiff()
        assertContains(diff, " line 7"); assertContains(diff, "-line 10"); assertContains(diff, "+changed 10")
        assertFalse(diff.contains(" line 1\n")); assertFalse(diff.contains(" line 20"))
    }

    @Test fun `dry run never writes and confirmation creates file`() {
        val root = root(); val tools = ProjectFileTools(root)
        tools.proposeWrite("report.md", "one")
        assertTrue(tools.applyConfirmed(true, true).isEmpty()); assertFalse(Files.exists(root.resolve("report.md")))
        assertEquals(listOf("report.md"), tools.applyConfirmed(true, false)); assertEquals("one", root.resolve("report.md").readText())
    }

    @Test fun `patch checks expected text and applies after confirmation`() {
        val root = root(); root.resolve("README.md").writeText("old value")
        val tools = ProjectFileTools(root); tools.proposePatch("README.md", "old", "new")
        assertContains(tools.getDiff(), "+new value"); tools.applyConfirmed(true, false)
        assertEquals("new value", root.resolve("README.md").readText())
        assertFailsWith<PatchConflict> { ProjectFileTools(root).proposePatch("README.md", "missing", "x") }
    }
}
