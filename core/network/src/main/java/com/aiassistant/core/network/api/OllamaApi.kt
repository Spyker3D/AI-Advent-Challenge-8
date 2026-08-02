package com.aiassistant.core.network.api

import com.aiassistant.core.network.dto.OllamaGenerateRequestDto
import com.aiassistant.core.network.dto.OllamaGenerateResponseDto
import com.aiassistant.core.network.dto.OllamaEmbedRequestDto
import com.aiassistant.core.network.dto.OllamaEmbedResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {
    @POST("/api/embed")
    suspend fun embed(
        @Body request: OllamaEmbedRequestDto
    ): OllamaEmbedResponseDto

    @POST("/api/generate")
    suspend fun generate(
        @Body request: OllamaGenerateRequestDto
    ): OllamaGenerateResponseDto
}
