package com.aiassistant.core.domain.memory

import javax.inject.Inject

class TaskMemoryMerger @Inject constructor() {
    fun merge(taskContext: TaskContext, update: TaskContextUpdate): TaskContext {
        val currentState = mergeCurrentState(
            existing = taskContext.currentState,
            replacement = update.currentState,
            clarifications = update.clarificationsAdd,
            terms = update.termsAdd
        )

        return taskContext.copy(
            goals = mergeList(taskContext.goals, update.goalsAdd),
            constraints = mergeList(taskContext.constraints, update.constraintsAdd),
            decisions = mergeList(taskContext.decisions, update.decisionsAdd),
            currentState = currentState
        )
    }

    private fun mergeList(existing: List<String>, additions: List<String>): List<String> {
        val normalized = linkedMapOf<String, String>()
        (existing + additions)
            .mapNotNull { value -> value.normalizeMemoryLine() }
            .forEach { value ->
                normalized.putIfAbsent(value.lowercase(), value)
            }
        return normalized.values.take(MAX_ITEMS)
    }

    private fun mergeCurrentState(
        existing: String,
        replacement: String?,
        clarifications: List<String>,
        terms: List<String>
    ): String {
        val existingParts = parseCurrentState(existing)
        val existingClarifications = existingParts.clarifications
        val existingTerms = existingParts.terms
        val base = replacement?.normalizeMemoryLine() ?: existingParts.base.trim()
        val sections = mutableListOf<String>()
        if (base.isNotBlank()) sections += base

        val normalizedClarifications = mergeList(existingClarifications, clarifications)
        if (normalizedClarifications.isNotEmpty()) {
            sections += buildSection("Clarifications", normalizedClarifications)
        }

        val normalizedTerms = mergeList(existingTerms, terms)
        if (normalizedTerms.isNotEmpty()) {
            sections += buildSection("Terms", normalizedTerms)
        }

        return sections.joinToString(separator = "\n\n").trim()
    }

    private fun buildSection(title: String, values: List<String>): String {
        return buildString {
            appendLine("$title:")
            values.forEach { value -> appendLine("- $value") }
        }.trim()
    }

    private fun String.normalizeMemoryLine(): String? {
        val normalized = replace(Regex("\\s+"), " ").trim()
        return normalized.takeIf { it.isNotBlank() }
    }

    private fun parseCurrentState(value: String): CurrentStateParts {
        val baseLines = mutableListOf<String>()
        val clarifications = mutableListOf<String>()
        val terms = mutableListOf<String>()
        var section: ManagedSection? = null

        value.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.equals("Clarifications:", ignoreCase = true) -> {
                    section = ManagedSection.CLARIFICATIONS
                }
                trimmed.equals("Terms:", ignoreCase = true) -> {
                    section = ManagedSection.TERMS
                }
                section != null && trimmed.startsWith("-") -> {
                    val item = trimmed.removePrefix("-").trim().normalizeMemoryLine()
                    if (item != null) {
                        when (section) {
                            ManagedSection.CLARIFICATIONS -> clarifications += item
                            ManagedSection.TERMS -> terms += item
                            null -> Unit
                        }
                    }
                }
                section != null && trimmed.isBlank() -> Unit
                else -> {
                    section = null
                    baseLines += line
                }
            }
        }

        return CurrentStateParts(
            base = baseLines.joinToString(separator = "\n").trim(),
            clarifications = clarifications,
            terms = terms
        )
    }

    companion object {
        private const val MAX_ITEMS = 20
    }

    private enum class ManagedSection {
        CLARIFICATIONS,
        TERMS
    }

    private data class CurrentStateParts(
        val base: String,
        val clarifications: List<String>,
        val terms: List<String>
    )
}
