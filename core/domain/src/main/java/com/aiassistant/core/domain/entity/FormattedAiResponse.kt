package com.aiassistant.core.domain.entity

data class FormattedAiResponse(
    val topic: String,
    val date: String,
    val time: String,
    val answer: String,
    val tags: List<String>
)