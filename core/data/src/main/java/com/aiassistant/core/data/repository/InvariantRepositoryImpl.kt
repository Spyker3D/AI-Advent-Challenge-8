package com.aiassistant.core.data.repository

import android.content.Context
import com.aiassistant.core.domain.invariant.ArchitectureInvariant
import com.aiassistant.core.domain.invariant.BudgetInvariant
import com.aiassistant.core.domain.invariant.Invariant
import com.aiassistant.core.domain.invariant.MaxDependenciesInvariant
import com.aiassistant.core.domain.invariant.StackInvariant
import com.aiassistant.core.domain.invariant.defaultInvariants
import com.aiassistant.core.domain.repository.InvariantRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class InvariantRepositoryImpl @Inject constructor(
    private val context: Context,
    private val gson: Gson
) : InvariantRepository {

    private val invariantsFile: File
        get() = File(context.filesDir, "memory/invariants/invariants.json")

    override suspend fun getInvariants(): List<Invariant> = withContext(Dispatchers.IO) {
        ensureFile()
        runCatching { parse(gson.fromJson(invariantsFile.readText(), JsonArray::class.java)) }
            .getOrElse {
                val defaults = defaultInvariants()
                write(defaults)
                defaults
            }
    }

    override suspend fun saveInvariants(invariants: List<Invariant>) =
        withContext(Dispatchers.IO) {
            write(invariants)
        }

    private fun ensureFile() {
        invariantsFile.parentFile?.mkdirs()
        if (!invariantsFile.exists()) write(defaultInvariants())
    }

    private fun write(invariants: List<Invariant>) {
        invariantsFile.parentFile?.mkdirs()
        val json = JsonArray()
        invariants.forEach { invariant ->
            json.add(JsonObject().apply {
                addProperty("type", invariant.id)
                when (invariant) {
                    is StackInvariant -> {
                        add("allowed", gson.toJsonTree(invariant.allowed))
                        add("banned", gson.toJsonTree(invariant.banned))
                    }
                    is ArchitectureInvariant -> {
                        addProperty("required", invariant.required)
                        add("banned", gson.toJsonTree(invariant.banned))
                    }
                    is BudgetInvariant -> addProperty("rule", invariant.rule)
                    is MaxDependenciesInvariant -> addProperty("max", invariant.max)
                }
            })
        }
        invariantsFile.writeText(gson.toJson(json))
    }

    private fun parse(json: JsonArray): List<Invariant> = json.mapNotNull { element ->
        val item = element.asJsonObject
        when (item.get("type")?.asString) {
            "stack" -> StackInvariant(
                allowed = item.stringSet("allowed"),
                banned = item.stringSet("banned")
            )
            "architecture" -> ArchitectureInvariant(
                required = item.get("required")?.asString
                    ?: item.get("architecture")?.asString
                    ?: "MVVM",
                banned = item.stringSet("banned")
            )
            "budget" -> BudgetInvariant(rule = item.get("rule").asString)
            "max_dependencies" -> MaxDependenciesInvariant(max = item.get("max").asInt)
            else -> null
        }
    }.ifEmpty { defaultInvariants() }

    private fun JsonObject.stringSet(name: String): Set<String> =
        getAsJsonArray(name)?.mapTo(linkedSetOf()) { it.asString } ?: emptySet()
}
