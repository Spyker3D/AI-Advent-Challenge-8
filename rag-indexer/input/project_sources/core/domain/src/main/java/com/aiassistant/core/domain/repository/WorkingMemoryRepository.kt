package com.aiassistant.core.domain.repository

import com.aiassistant.core.domain.memory.TaskContext

interface WorkingMemoryRepository {
    suspend fun getTaskContext(id: String): TaskContext?
    suspend fun getActiveTaskContext(): TaskContext?
    suspend fun setActiveTaskContext(id: String)
    suspend fun saveTaskContext(taskContext: TaskContext)
    suspend fun clearActiveTaskContext()
}
