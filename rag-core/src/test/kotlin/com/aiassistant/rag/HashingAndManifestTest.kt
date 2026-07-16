package com.aiassistant.rag

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HashingAndManifestTest {
    @Test fun `sha256 is stable and changes with content`() {
        assertEquals(FileHash.sha256("same"), FileHash.sha256("same"))
        assertNotEquals(FileHash.sha256("same"), FileHash.sha256("changed"))
    }

    @Test fun `manifest diff classifies all file states`() {
        val old = IndexManifest(mapOf(
            "unchanged.kt" to ManifestEntry("a", 1, emptyList()),
            "changed.kt" to ManifestEntry("b", 1, emptyList()),
            "deleted.kt" to ManifestEntry("c", 1, emptyList())
        ))
        val diff = ManifestDiffer.diff(old, mapOf("unchanged.kt" to "a", "changed.kt" to "new", "new.kt" to "d"))
        assertEquals(setOf("new.kt"), diff.newFiles)
        assertEquals(setOf("changed.kt"), diff.changedFiles)
        assertEquals(setOf("deleted.kt"), diff.deletedFiles)
        assertEquals(setOf("unchanged.kt"), diff.unchangedFiles)
    }
}
