package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.inference.IncidentCategory

interface EmbeddingClient {
    suspend fun embed(texts: List<String>, model: String): Result<List<List<Float>>>
}

interface MicroPrototypeProvider {
    suspend fun loadPrototypes(): Result<Map<IncidentCategory, List<String>>>
}
