package com.aiassistant.core.data.mapper

import com.aiassistant.core.data.database.ChatMessageEntity
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.TokenMetrics
import javax.inject.Inject

class ChatMessageMapper @Inject constructor() {

    fun toEntity(message: Message, branchId: String = "main"): ChatMessageEntity {
        return ChatMessageEntity(
            id = message.id,
            role = message.role.value,
            content = message.content,
            timestamp = message.timestamp,
            currentRequestTokens = message.tokenMetrics?.currentRequestTokens,
            historyTokens = message.tokenMetrics?.historyTokens,
            completionTokens = message.tokenMetrics?.completionTokens,
            branchId = branchId
        )
    }

    fun toDomain(entity: ChatMessageEntity): Message {
        val tokenMetrics = if (entity.currentRequestTokens != null || entity.historyTokens != null || entity.completionTokens != null) {
            TokenMetrics(
                currentRequestTokens = entity.currentRequestTokens ?: 0,
                historyTokens = entity.historyTokens ?: 0,
                completionTokens = entity.completionTokens
            )
        } else {
            null
        }
        
        return Message(
            id = entity.id,
            content = entity.content,
            role = when (entity.role.lowercase()) {
                "user" -> MessageRole.USER
                "assistant" -> MessageRole.ASSISTANT
                "system" -> MessageRole.SYSTEM
                else -> MessageRole.USER // fallback to user role
            },
            timestamp = entity.timestamp,
            tokenMetrics = tokenMetrics
        )
    }
}