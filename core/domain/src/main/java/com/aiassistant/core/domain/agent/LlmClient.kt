package com.aiassistant.core.domain.agent

import com.aiassistant.core.domain.entity.Message

interface LlmClient {
    suspend fun sendChat(messages: List<Message>): Result<String>
}