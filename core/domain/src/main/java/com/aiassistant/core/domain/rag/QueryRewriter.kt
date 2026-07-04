package com.aiassistant.core.domain.rag

interface QueryRewriter {
    suspend fun rewrite(question: String): String
}
