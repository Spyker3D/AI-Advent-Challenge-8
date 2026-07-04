package com.aiassistant.core.network.api

import com.aiassistant.core.network.dto.ChatRequestDto
import com.aiassistant.core.network.dto.ChatResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun sendChatMessage(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatRequestDto
    ): Response<ChatResponseDto>
}