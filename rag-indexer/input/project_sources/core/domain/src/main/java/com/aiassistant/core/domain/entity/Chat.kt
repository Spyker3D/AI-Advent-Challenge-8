package com.aiassistant.core.domain.entity

data class Chat(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessagePreview: String = "",
    val activeTaskContextId: String? = null
)
