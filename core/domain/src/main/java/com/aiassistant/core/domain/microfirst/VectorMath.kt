package com.aiassistant.core.domain.microfirst

import kotlin.math.sqrt

internal object VectorMath {
    fun normalize(vector: List<Float>): List<Double>? {
        if (vector.isEmpty() || vector.any { !it.isFinite() }) return null
        val norm = sqrt(vector.sumOf { it.toDouble() * it.toDouble() })
        if (!norm.isFinite() || norm == 0.0) return null
        return vector.map { it / norm }
    }

    fun centroid(vectors: List<List<Double>>): List<Double>? {
        if (vectors.isEmpty()) return null
        val dimension = vectors.first().size
        if (dimension == 0 || vectors.any { it.size != dimension }) return null
        val average = List(dimension) { index -> vectors.sumOf { it[index] } / vectors.size }
        val norm = sqrt(average.sumOf { it * it })
        if (!norm.isFinite() || norm == 0.0) return null
        return average.map { it / norm }
    }

    fun cosine(left: List<Double>, right: List<Double>): Double? {
        if (left.isEmpty() || left.size != right.size) return null
        return left.indices.sumOf { left[it] * right[it] }.takeIf(Double::isFinite)
    }
}
