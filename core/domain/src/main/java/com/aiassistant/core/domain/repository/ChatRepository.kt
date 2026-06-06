package com.aiassistant.core.domain.repository

import com.aiassistant.core.domain.entity.ChatRequest

interface ChatRepository {
    suspend fun sendMessage(chatRequest: ChatRequest): Result<String>
}