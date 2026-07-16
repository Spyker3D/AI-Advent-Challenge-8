package com.aiassistant.core.network.api

import com.aiassistant.core.network.dto.OpenAiResponseDto
import com.aiassistant.core.network.dto.OpenAiResponseRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/responses")
    suspend fun createResponse(@Body request: OpenAiResponseRequestDto): OpenAiResponseDto
}
