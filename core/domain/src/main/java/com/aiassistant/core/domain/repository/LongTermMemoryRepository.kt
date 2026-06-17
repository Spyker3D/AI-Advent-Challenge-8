package com.aiassistant.core.domain.repository

import com.aiassistant.core.domain.memory.LongTermMemory

interface LongTermMemoryRepository {
    suspend fun getLongTermMemory(): LongTermMemory
    suspend fun saveProfile(content: String)
    suspend fun saveGlobalRules(content: String)
    suspend fun saveProjectKnowledge(content: String)
    suspend fun saveDecisions(content: String)
}
