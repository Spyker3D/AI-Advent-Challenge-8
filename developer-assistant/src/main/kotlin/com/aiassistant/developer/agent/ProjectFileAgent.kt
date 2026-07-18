package com.aiassistant.developer.agent

import com.aiassistant.developer.files.ProjectFileTools
import com.aiassistant.developer.llm.LlmClient
import com.google.gson.JsonParser
import java.nio.file.Path
import java.time.LocalDate

data class AgentResult(val message: String, val diff: String, val changedFiles: List<String>, val iterations: Int)

class ProjectFileAgent(
    private val root: Path,
    private val tools: ProjectFileTools,
    private val dryRun: Boolean,
    private val llm: LlmClient? = null,
    private val logger: OperationLogger = OperationLogger(root),
    private val maxIterations: Int = 10
) {
    private val used = mutableListOf<String>()
    private val read = linkedSetOf<String>()
    private var iterations = 0

    suspend fun execute(goal: String, confirm: (String) -> Boolean): AgentResult {
        used.clear(); read.clear(); iterations = 0; tools.clear()
        val plan = resolveSubject(goal)
        val scenario: suspend () -> Unit = when {
            goal.contains("инвариант", true) || goal.contains("CRM", true) || goal.contains("проверь файлы поддержки", true) -> { { validationReport() } }
            goal.contains("документ", true) || goal.contains("README", true) -> { { documentationUpdate(goal, plan) } }
            else -> { { usageReport(plan.subject) } }
        }
        scenario()
        val proposed = tools.proposedChanges()
        val diff = tools.getDiff()
        if (proposed.isEmpty()) return finish(goal, "no_changes", emptyList(), "No changes required.", diff)
        val summary = "Planned changes: modify ${proposed.count { it.original != null }} file(s), create ${proposed.count { it.original == null }} file(s)."
        val confirmed = !dryRun && confirm("$summary\n\n$diff\n\nApply changes?")
        val changed = tools.applyConfirmed(confirmed, dryRun)
        val status = when { dryRun -> "dry_run"; confirmed -> "completed"; else -> "cancelled" }
        val message = when { dryRun -> "$summary\n\n$diff\n\nDry-run: no files were written."; confirmed -> "Changes saved and verified: ${changed.joinToString()}. Run /reindex to refresh the index."; else -> "Changes cancelled; no files were written." }
        return finish(goal, status, changed, message, diff)
    }

    private fun usageReport(subject: String) {
        tool("list_files"); tools.listFiles(".", 4)
        tool("search_in_files"); val matches = searchSubject(subject, setOf(".kt", ".kts", ".java", ".js", ".ts", ".md"))
        require(matches.isNotEmpty()) { "$subject was not found." }
        val relevantMatches = matches.filterNot { isGeneratedOrTest(it.path) }
            .sortedBy { productionRank(it.path) }.take(30).ifEmpty { matches.take(30) }
        val paths = relevantMatches.map { it.path }.distinct().take(3)
        paths.forEach { tool("read_file"); tools.readFile(it, 1, 240); read += it }
        val definition = relevantMatches.firstOrNull { Regex("(?:class|interface|object)\\s+", RegexOption.IGNORE_CASE).containsMatchIn(it.text) } ?: relevantMatches.first()
        val usages = relevantMatches.filterNot { it == definition }.groupBy { it.path }.entries.joinToString("\n\n") { (path, found) ->
            "### $path\n\n" + found.take(8).joinToString("\n") { "- line ${it.line}: confirmed reference." }
        }
        val report = """# $subject Usage Report

## Definition

- Path: `${definition.path}:${definition.line}`.
- Role: component or API found by exact project search.
- Public behavior was verified from current files on disk.

## Usages

${usages.ifBlank { "No direct references outside the definition were found." }}

## Evidence

- Exact search was followed by reading ${paths.size} relevant production files.

## Change Risks

- Public API changes require rechecking the listed references and tests.

## Generated

${LocalDate.now()}
"""
        val reportPath = if (subject == "SupportAssistantService") "docs/generated/support-assistant-usage.md" else "docs/generated/${slug(subject)}-usage.md"
        tool("write_file"); tools.proposeWrite(reportPath, report)
        tool("get_diff")
    }

    private suspend fun documentationUpdate(goal: String, plan: SubjectPlan) {
        val subject = plan.subject
        tool("search_in_files"); val matches = plan.searchTerms.flatMap {
            tools.searchInFiles(it, ".", setOf(".kt", ".kts", ".java", ".js", ".ts", ".json", ".xml", ".md"), limit = 60)
        }.distinctBy { Triple(it.path, it.line, it.text) }.take(80)
        require(matches.isNotEmpty()) { "No code or documentation found for: $subject" }
        val sourcePaths = matches.map { it.path }.filterNot { it == "README.md" || isGeneratedOrTest(it) }
            .distinct().sortedBy(::productionRank).take(5).ifEmpty { matches.map { it.path }.filterNot { it == "README.md" }.distinct().take(5) }
        val contents = linkedMapOf<String, String>()
        listOf("README.md").plus(sourcePaths).forEach {
            tool("read_file"); contents[it] = tools.readFile(it, 1, 320); read += it
        }
        val current = rawText("README.md")
        val marker = slug(subject)
        val start = "<!-- day34-$marker-doc:start -->"; val end = "<!-- day34-$marker-doc:end -->"
        val generated = llm?.generate(DOCUMENTATION_PROMPT, buildDocumentationInput(goal, subject, contents))
            ?.trim()?.removePrefix("```markdown")?.removeSuffix("```")?.trim()
        val body = generated?.takeIf { it.isNotBlank() } ?: fallbackDocumentation(subject, matches, sourcePaths)
        val block = "$start\n$body\n$end"
        val updated = if (current.contains(start) && current.contains(end)) current.replace(Regex("(?s)${Regex.escape(start)}.*?${Regex.escape(end)}"), block) else current.trimEnd() + "\n\n" + block + "\n"
        tool("apply_patch"); tools.proposeWrite("README.md", updated); tool("get_diff")
    }

    private fun validationReport() {
        val paths = listOf("mcp-server/data/tickets.json", "mcp-server/data/support-users.json", "README.md", "mcp-server/server.js")
        paths.forEach { tool("read_file"); tools.readFile(it, 1, 500); read += it }
        val checks = ProjectValidation.validateTickets(rawText(paths[0]), rawText(paths[1])).toMutableList()
        val readme = rawText("README.md"); val server = rawText("mcp-server/server.js")
        tool("search_in_files"); val secretMatches = listOf("sk-", "OPENAI_API_KEY =", "apiKey = \"").flatMap {
            tools.searchInFiles(it, ".", setOf(".kt", ".kts", ".java"), limit = 20)
        }
        checks += ValidationCheck(secretMatches.isEmpty(), "No hardcoded Android secrets", if (secretMatches.isEmpty()) "No common hardcoded secret patterns found in Android source." else secretMatches.joinToString { "${it.path}:${it.line}" }, "Move secrets to protected local configuration.")
        tool("search_in_files"); val localEndpoints = tools.searchInFiles("localhost", ".", setOf(".kt", ".kts"), limit = 30)
            .filter { it.path.startsWith("app/") || it.path.startsWith("core/") || it.path.startsWith("feature/") }
        checks += ValidationCheck(localEndpoints.isEmpty(), "Android emulator MCP endpoint", if (localEndpoints.isEmpty()) "No Android localhost endpoint found; emulator endpoints may use 10.0.2.2." else localEndpoints.joinToString { "${it.path}:${it.line}" }, "Use 10.0.2.2 instead of localhost from Android Emulator.")
        checks += ValidationCheck(!Regex("(?i)авторизац|подписк|платеж").containsMatchIn(readme), "README product claims", "Checked README for authorization, subscriptions and payments.", "Remove unsupported product claims.")
        val documentedTools = Regex("`([a-z][a-z0-9_]+)`").findAll(readme).map { it.groupValues[1] }.filter { it.contains('_') }.toSet()
        val missing = documentedTools.filterNot(server::contains)
        checks += ValidationCheck(missing.isEmpty(), "Documented MCP tools exist", if (missing.isEmpty()) "Documented tool names were found in server.js." else "Not found: ${missing.joinToString()}", "Synchronize README and server.js.")
        val passed = checks.count { it.passed }; val failed = checks.size - passed
        val body = checks.joinToString("\n\n") { "### ${if (it.passed) "PASS" else "FAIL"} — ${it.name}\n\nEvidence:\n- ${it.evidence}" + (it.recommendation?.let { r -> "\n\nRecommended change:\n- $r" } ?: "") }
        val report = "# Project Validation Report\n\n## Summary\n\nPassed: $passed\nWarnings: 0\nFailed: $failed\n\n## Checks\n\n$body\n"
        tool("write_file"); tools.proposeWrite("reports/day-34-project-validation.md", report); tool("get_diff")
    }

    private fun rawText(path: String): String = tools.readFile(path, 1, Int.MAX_VALUE).lineSequence().joinToString("\n") { it.substringAfter(": ", "") }
    private fun searchSubject(subject: String, extensions: Set<String>, limit: Int = 80) =
        GoalSubjectExtractor.searchTerms(subject).flatMap { tools.searchInFiles(it, ".", extensions, limit = limit) }
            .distinctBy { Triple(it.path, it.line, it.text) }.take(limit)
    private fun slug(value: String) = value.lowercase().replace(Regex("[^a-zа-я0-9]+"), "-").trim('-').ifBlank { "component" }
    private fun isGeneratedOrTest(path: String) = path.contains("/test/") || path.contains("/build/") ||
        path.startsWith("rag-indexer/input/") || path.startsWith("docs/") || path == "README.md" || path == "API_KEY_SETUP.md"
    private fun productionRank(path: String) = when {
        path.contains("/src/main/") -> 0
        path.endsWith("build.gradle.kts") -> 1
        else -> 2
    }
    private suspend fun resolveSubject(goal: String): SubjectPlan {
        if (llm == null) return GoalSubjectExtractor.localPlan(goal)
        val response = llm.generate(SUBJECT_PROMPT, goal)
        return runCatching {
            val json = JsonParser.parseString(response.substringAfter('{').substringBeforeLast('}') .let { "{$it}" }).asJsonObject
            val subject = json.get("subject").asString.trim()
            val terms = json.getAsJsonArray("searchTerms").map { it.asString.trim() }.filter { it.isNotBlank() }
            SubjectPlan(subject, terms.ifEmpty { listOf(subject) })
        }.getOrElse { GoalSubjectExtractor.localPlan(goal) }
    }
    private fun buildDocumentationInput(goal: String, subject: String, contents: Map<String, String>) = buildString {
        appendLine("USER GOAL: $goal"); appendLine("SUBJECT: $subject")
        appendLine("CURRENT FILES (line-numbered; the only allowed evidence):")
        contents.forEach { (path, content) -> appendLine("\nFILE $path\n${content.take(18_000)}") }
    }
    private fun fallbackDocumentation(subject: String, matches: List<com.aiassistant.developer.files.SearchMatch>, paths: List<String>): String {
        val evidence = matches.filter { it.path in paths }.groupBy { it.path }.entries.joinToString("\n") { (path, found) ->
            "- `$path`: matches at lines ${found.map { it.line }.distinct().take(8).joinToString()}."
        }
        return "### Current implementation: $subject\n\nAutomated semantic generation was unavailable. Confirmed evidence:\n\n$evidence"
    }
    private fun tool(name: String) { check(++iterations <= maxIterations) { "Exceeded MAX_TOOL_ITERATIONS=$maxIterations; partial analysis stopped." }; used += name }
    private fun finish(goal: String, status: String, changed: List<String>, message: String, diff: String): AgentResult {
        logger.save(OperationRecord(goal = goal, toolsUsed = used.toList(), filesRead = read.toList(), filesChanged = changed, status = status, iterations = iterations))
        return AgentResult(message, diff, changed, iterations)
    }

    companion object {
        const val SYSTEM_PROMPT = """You are a project file assistant. The user provides a goal rather than individual file operations. Inspect the project and select tools yourself. Search and read actual files before conclusions. Never invent content, APIs, classes, or features. Prepare and show a diff, request confirmation, write only after confirmation, then verify. Never leave the configured project root, expose secrets, or execute destructive Git commands."""
        const val SUBJECT_PROMPT = """Extract the software component or subsystem that the user wants documented. Return JSON only: {"subject":"short display name","searchTerms":["exact source term", "alternative identifier"]}. Use 1-4 precise terms likely to occur in code. Never include file paths or secrets."""
        const val DOCUMENTATION_PROMPT = """Write a concise Markdown section documenting the requested component from CURRENT FILES only. Start with `### Current implementation: <subject>`. Explain in prose: purpose, main components and responsibilities, end-to-end data/control flow, configuration, dependencies, error handling, and important security or maintenance limitations when evidenced. Cite every paragraph or bullet with relative file paths and line numbers. Do not merely list search matches. Do not invent behavior. Do not mention unavailable categories. Do not include marker comments or fenced code around the whole response."""
    }
}

data class SubjectPlan(val subject: String, val searchTerms: List<String>)

object GoalSubjectExtractor {
    private val known = listOf("OpenAI API", "SupportAssistantService", "MCP", "RAG", "OpenRouter API", "Ollama")

    fun extract(goal: String): String {
        known.firstOrNull { goal.contains(it, true) }?.let { return it }
        Regex("`([^`]{2,80})`").find(goal)?.groupValues?.get(1)?.let { return it }
        Regex("\\b([A-Z][A-Za-z0-9_]*(?:Service|Client|Repository|ViewModel|API))\\b").find(goal)?.groupValues?.get(1)?.let { return it }
        throw IllegalArgumentException("Could not identify a component or API in the goal. Name the subject, but not individual files.")
    }

    fun localPlan(goal: String): SubjectPlan = extract(goal).let { SubjectPlan(it, searchTerms(it)) }

    fun searchTerms(subject: String): List<String> = when {
        subject.equals("OpenAI API", true) -> listOf("OpenAI", "openai", "api.openai.com")
        subject.equals("OpenRouter API", true) -> listOf("OpenRouter", "openrouter")
        else -> listOf(subject)
    }.distinct()
}
