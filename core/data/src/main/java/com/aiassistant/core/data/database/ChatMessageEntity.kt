package com.aiassistant.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val currentRequestTokens: Int? = null,
    val historyTokens: Int? = null,
    val completionTokens: Int? = null,
    val branchId: String = "main"  // Default to "main" branch
)