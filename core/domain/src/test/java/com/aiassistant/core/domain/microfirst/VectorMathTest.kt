package com.aiassistant.core.domain.microfirst

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorMathTest {
    @Test fun `identical normalized vectors have cosine one`() {
        val vector = VectorMath.normalize(listOf(3f, 4f))!!
        assertEquals(1.0, VectorMath.cosine(vector, vector)!!, 1e-12)
    }

    @Test fun `cosine rejects dimension mismatch`() {
        assertNull(VectorMath.cosine(listOf(1.0), listOf(1.0, 0.0)))
    }

    @Test fun `normalization rejects zero non finite and empty vectors`() {
        assertNull(VectorMath.normalize(emptyList()))
        assertNull(VectorMath.normalize(listOf(0f, 0f)))
        assertNull(VectorMath.normalize(listOf(Float.NaN)))
        assertNull(VectorMath.normalize(listOf(Float.POSITIVE_INFINITY)))
        assertNull(VectorMath.normalize(listOf(Float.NEGATIVE_INFINITY)))
    }

    @Test fun `centroid averages normalized vectors then normalizes result`() {
        val centroid = VectorMath.centroid(listOf(listOf(1.0, 0.0), listOf(0.0, 1.0)))!!
        val expected = 1.0 / kotlin.math.sqrt(2.0)
        assertEquals(expected, centroid[0], 1e-12)
        assertEquals(expected, centroid[1], 1e-12)
        assertTrue(VectorMath.centroid(listOf(listOf(1.0), listOf(1.0, 0.0))) == null)
    }
}
