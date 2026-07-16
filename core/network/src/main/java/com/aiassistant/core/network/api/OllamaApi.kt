package com.aiassistant.core.network.api

import com.aiassistant.core.network.dto.OllamaGenerateRequestDto
import com.aiassistant.core.network.dto.OllamaGenerateResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {
    @POST("/api/generate")
    suspend fun generate(
        @Body request: OllamaGenerateRequestDto
    ): OllamaGenerateResponseDto
}
