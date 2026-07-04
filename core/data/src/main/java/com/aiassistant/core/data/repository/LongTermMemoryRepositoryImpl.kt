package com.aiassistant.core.data.repository

import android.content.Context
import com.aiassistant.core.domain.memory.LongTermMemory
import com.aiassistant.core.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class LongTermMemoryRepositoryImpl @Inject constructor(
    private val context: Context
) : LongTermMemoryRepository {

    private val longTermDir: File
        get() = File(context.filesDir, "memory/long_term")

    override suspend fun getLongTermMemory(): LongTermMemory = withContext(Dispatchers.IO) {
        ensureFiles()
        LongTermMemory(
            profile = file(PROFILE).readText(),
            preferences = file(PREFERENCES).readText(),
            globalRules = file(GLOBAL_RULES).readText(),
            projectKnowledge = file(PROJECT_KNOWLEDGE).readText(),
            decisions = file(DECISIONS).readText()
        )
    }

    override suspend fun getPreferences(): String = withContext(Dispatchers.IO) {
        ensureFiles()
        file(PREFERENCES).readText()
    }

    override suspend fun saveProfile(content: String) = write(PROFILE, content)
    override suspend fun savePreferences(content: String) = write(PREFERENCES, content)
    override suspend fun saveGlobalRules(content: String) = write(GLOBAL_RULES, content)
    override suspend fun saveProjectKnowledge(content: String) = write(PROJECT_KNOWLEDGE, content)
    override suspend fun saveDecisions(content: String) = write(DECISIONS, content)

    private suspend fun write(name: String, content: String) = withContext(Dispatchers.IO) {
        ensureFiles()
        file(name).writeText(content)
    }

    private fun ensureFiles() {
        longTermDir.mkdirs()
        ensureFile(PROFILE, "# Profile\n")
        ensureFile(
            PREFERENCES,
            """
            # Preferences
            
            Language:
            - Russian
            
            Answer style:
            - concise
            
            Code style:
            - prefer Kotlin
            """.trimIndent() + "\n"
        )
        ensureFile(GLOBAL_RULES, "# Global Rules\n")
        ensureFile(PROJECT_KNOWLEDGE, "# Project Knowledge\n")
        ensureFile(DECISIONS, "# Decisions\n")
    }

    private fun ensureFile(name: String, defaultContent: String) {
        val target = file(name)
        if (!target.exists()) {
            target.writeText(defaultContent)
        }
    }

    private fun file(name: String): File = File(longTermDir, name)

    private companion object {
        const val PROFILE = "profile.md"
        const val PREFERENCES = "preferences.md"
        const val GLOBAL_RULES = "global_rules.md"
        const val PROJECT_KNOWLEDGE = "project_knowledge.md"
        const val DECISIONS = "decisions.md"
    }
}
