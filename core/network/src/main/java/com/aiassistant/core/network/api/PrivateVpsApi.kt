package com.aiassistant.core.network.api

import com.aiassistant.core.network.dto.PrivateVpsChatRequestDto
import com.aiassistant.core.network.dto.PrivateVpsChatResponseDto
import com.aiassistant.core.network.dto.PrivateVpsModelsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface PrivateVpsApi {
    @GET suspend fun getModels(@Url url: String): PrivateVpsModelsResponseDto
    @POST suspend fun createChatCompletion(
        @Url url: String,
        @Body request: PrivateVpsChatRequestDto
    ): PrivateVpsChatResponseDto
}
