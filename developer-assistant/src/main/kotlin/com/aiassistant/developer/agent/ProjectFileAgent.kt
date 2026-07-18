package com.aiassistant.developer.agent

import com.aiassistant.developer.files.ProjectFileTools
import java.nio.file.Path
import java.time.LocalDate

data class AgentResult(val message: String, val diff: String, val changedFiles: List<String>, val iterations: Int)

class ProjectFileAgent(
    private val root: Path,
    private val tools: ProjectFileTools,
    private val dryRun: Boolean,
    private val logger: OperationLogger = OperationLogger(root),
    private val maxIterations: Int = 10
) {
    private val used = mutableListOf<String>()
    private val read = linkedSetOf<String>()
    private var iterations = 0

    fun execute(goal: String, confirm: (String) -> Boolean): AgentResult {
        used.clear(); read.clear(); iterations = 0; tools.clear()
        val subject = GoalSubjectExtractor.extract(goal)
        val scenario: () -> Unit = when {
            goal.contains("инвариант", true) || goal.contains("CRM", true) || goal.contains("проверь файлы поддержки", true) -> ::validationReport
            goal.contains("документ", true) || goal.contains("README", true) -> { { documentationUpdate(subject) } }
            isUsageGoal(goal) -> { { usageReport(subject) } }
            else -> throw IllegalArgumentException("Цель не распознана. Укажите желаемый результат: найти использования компонента/API, обновить документацию или проверить инварианты.")
        }
        scenario()
        val proposed = tools.proposedChanges()
        val diff = tools.getDiff()
        if (proposed.isEmpty()) return finish(goal, "no_changes", emptyList(), "Изменения не требуются.", diff)
        val summary = "Планируется изменить ${proposed.count { it.original != null }} файла(ов) и создать ${proposed.count { it.original == null }} файла(ов)."
        val confirmed = !dryRun && confirm("$summary\n\n$diff\n\nПрименить изменения?")
        val changed = tools.applyConfirmed(confirmed, dryRun)
        val status = when { dryRun -> "dry_run"; confirmed -> "completed"; else -> "cancelled" }
        val message = when { dryRun -> "$summary\nDry-run: файлы не записаны."; confirmed -> "Изменения сохранены и проверены: ${changed.joinToString()}. Индекс следует обновить командой /reindex."; else -> "Изменения отменены; файлы не записаны." }
        return finish(goal, status, changed, message, diff)
    }

    private fun usageReport(subject: String) {
        tool("list_files"); tools.listFiles(".", 4)
        tool("search_in_files"); val matches = searchSubject(subject, setOf(".kt", ".kts", ".java", ".js", ".ts", ".md"))
        require(matches.isNotEmpty()) { "$subject was not found." }
        val paths = matches.map { it.path }.distinct().take(3)
        paths.forEach { tool("read_file"); tools.readFile(it, 1, 240); read += it }
        val definition = matches.firstOrNull { Regex("(?:class|interface|object)\\s+", RegexOption.IGNORE_CASE).containsMatchIn(it.text) } ?: matches.first()
        val usages = matches.filterNot { it == definition }.joinToString("\n") { "### ${it.path}\n\n- строка ${it.line};\n- найденный вызов: `${it.text.replace("`", "'")}`." }
        val report = """# Использование $subject

## Определение

- путь: `${definition.path}:${definition.line}`;
- назначение: компонент или API, обнаруженный точным поиском по проекту;
- публичные методы следует проверять в актуальном исходном файле.

## Использования

${usages.ifBlank { "Прямые использования вне определения не найдены." }}

## Зависимости

- Отчёт построен по точному текстовому поиску и чтению ${paths.size} актуальных файлов.

## Риски при изменении

- Изменение публичного API требует повторной проверки перечисленных мест и тестов.

## Дата генерации

${LocalDate.now()}
"""
        val reportPath = if (subject == "SupportAssistantService") "docs/generated/support-assistant-usage.md" else "docs/generated/${slug(subject)}-usage.md"
        tool("write_file"); tools.proposeWrite(reportPath, report)
        tool("get_diff")
    }

    private fun documentationUpdate(subject: String) {
        tool("search_in_files"); val matches = searchSubject(subject, setOf(".kt", ".kts", ".java", ".js", ".ts", ".md"), 60)
        require(matches.isNotEmpty()) { "Не найден код или документация по теме: $subject" }
        val sourcePaths = matches.map { it.path }.filterNot { it == "README.md" }.distinct().take(3)
        listOf("README.md").plus(sourcePaths).forEach {
            tool("read_file"); tools.readFile(it, 1, 260); read += it
        }
        val current = rawText("README.md")
        val marker = slug(subject)
        val start = "<!-- day34-$marker-doc:start -->"; val end = "<!-- day34-$marker-doc:end -->"
        val evidence = matches.filter { it.path in sourcePaths }.groupBy { it.path }.entries.joinToString("\n") { (path, found) ->
            "- `$path` — совпадения на строках ${found.map { it.line }.distinct().take(8).joinToString()}."
        }
        val block = """$start
### Фактическая реализация: $subject

Раздел сформирован по актуальному коду проекта:

$evidence

Перечислены только файлы и строки, подтверждённые точным поиском и чтением с диска.
$end"""
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
    private fun isUsageGoal(goal: String) = Regex("(?i)использ|упомин|места|references?|usages?").containsMatchIn(goal) &&
        Regex("(?i)отч[её]т|найди|проанализируй|покажи|подготовь").containsMatchIn(goal)
    private fun slug(value: String) = value.lowercase().replace(Regex("[^a-zа-я0-9]+"), "-").trim('-').ifBlank { "component" }
    private fun tool(name: String) { check(++iterations <= maxIterations) { "Exceeded MAX_TOOL_ITERATIONS=$maxIterations; partial analysis stopped." }; used += name }
    private fun finish(goal: String, status: String, changed: List<String>, message: String, diff: String): AgentResult {
        logger.save(OperationRecord(goal = goal, toolsUsed = used.toList(), filesRead = read.toList(), filesChanged = changed, status = status, iterations = iterations))
        return AgentResult(message, diff, changed, iterations)
    }

    companion object {
        const val SYSTEM_PROMPT = """You are a project file assistant. The user provides a goal rather than individual file operations. Inspect the project and select tools yourself. Search and read actual files before conclusions. Never invent content, APIs, classes, or features. Prepare and show a diff, request confirmation, write only after confirmation, then verify. Never leave the configured project root, expose secrets, or execute destructive Git commands."""
    }
}

object GoalSubjectExtractor {
    private val known = listOf("OpenAI API", "SupportAssistantService", "MCP", "RAG", "OpenRouter API", "Ollama")

    fun extract(goal: String): String {
        known.firstOrNull { goal.contains(it, true) }?.let { return it }
        Regex("`([^`]{2,80})`").find(goal)?.groupValues?.get(1)?.let { return it }
        Regex("\\b([A-Z][A-Za-z0-9_]*(?:Service|Client|Repository|ViewModel|API))\\b").find(goal)?.groupValues?.get(1)?.let { return it }
        throw IllegalArgumentException("Не удалось определить компонент или API из цели. Назовите предмет анализа, но не конкретные файлы.")
    }

    fun searchTerms(subject: String): List<String> = when {
        subject.equals("OpenAI API", true) -> listOf("OpenAI", "openai", "api.openai.com")
        subject.equals("OpenRouter API", true) -> listOf("OpenRouter", "openrouter")
        else -> listOf(subject)
    }.distinct()
}
