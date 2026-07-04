package com.aiassistant.core.domain.entity

data class ChatBranch(
    val id: String,
    val name: String,
    val messages: List<Message> = emptyList()
)