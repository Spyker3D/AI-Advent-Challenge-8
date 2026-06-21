package com.aiassistant.core.data.repository

import android.content.Context
import com.aiassistant.core.domain.memory.TaskContext
import com.aiassistant.core.domain.memory.TaskState
import com.aiassistant.core.domain.repository.WorkingMemoryRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class WorkingMemoryRepositoryImpl @Inject constructor(
    private val context: Context,
    private val gson: Gson
) : WorkingMemoryRepository {

    private val workingDir: File
        get() = File(context.filesDir, "memory/working")

    private val tasksDir: File
        get() = File(workingDir, "tasks")

    private val activeTaskFile: File
        get() = File(workingDir, "active_task_context_id.txt")

    override suspend fun getTaskContext(id: String): TaskContext? = withContext(Dispatchers.IO) {
        ensureDirectories()
        val file = taskFile(id)
        if (!file.exists()) return@withContext null

        runCatching {
            val parsed = gson.fromJson(file.readText(), TaskContext::class.java)
            parsed.copy(
                relatedChatIds = parsed.relatedChatIds ?: emptyList(),
                goals = parsed.goals ?: emptyList(),
                constraints = parsed.constraints ?: emptyList(),
                decisions = parsed.decisions ?: emptyList(),
                currentState = parsed.currentState.orEmpty(),
                taskState = parsed.taskState ?: TaskState(),
                planningResult = parsed.planningResult.orEmpty(),
                executionResult = parsed.executionResult.orEmpty(),
                validationResult = parsed.validationResult.orEmpty(),
                planningSwarmResults = parsed.planningSwarmResults ?: emptyList()
            )
        }.getOrNull()
    }

    override suspend fun getActiveTaskContext(): TaskContext? = withContext(Dispatchers.IO) {
        ensureDirectories()
        val activeId = activeTaskFile
            .takeIf { it.exists() }
            ?.readText()
            ?.trim()
            .orEmpty()

        if (activeId.isBlank()) {
            null
        } else {
            getTaskContext(activeId)
        }
    }

    override suspend fun setActiveTaskContext(id: String) = withContext(Dispatchers.IO) {
        ensureDirectories()
        activeTaskFile.writeText(id)
    }

    override suspend fun saveTaskContext(taskContext: TaskContext) = withContext(Dispatchers.IO) {
        ensureDirectories()
        val normalized = taskContext.copy(updatedAt = System.currentTimeMillis())
        taskFile(normalized.id).writeText(gson.toJson(normalized))
    }

    override suspend fun clearActiveTaskContext() = withContext(Dispatchers.IO) {
        ensureDirectories()
        activeTaskFile.writeText("")
    }

    private fun ensureDirectories() {
        tasksDir.mkdirs()
        if (!activeTaskFile.exists()) {
            activeTaskFile.writeText("")
        }
    }

    private fun taskFile(id: String): File {
        val safeId = id.ifBlank { "memory_layers_assignment" }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(tasksDir, "$safeId.json")
    }
}
