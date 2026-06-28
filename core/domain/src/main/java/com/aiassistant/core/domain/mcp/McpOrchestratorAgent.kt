package com.aiassistant.core.domain.mcp

import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class McpOrchestratorAgent @Inject constructor(
    private val mcpAgentRepository: McpAgentRepository,
    private val llmClient: LlmClient
) {
    private val servers = McpServerRegistry.mcpServers

    fun canHandleOrchestration(userRequest: String): Boolean {
        val normalized = userRequest.lowercase()
        val hasWeather = listOf("погода", "погоде", "weather", "температур").any { normalized.contains(it) }
        val hasNotes = listOf("заметк", "notes", "сохрани").any { normalized.contains(it) }
        val hasTasks = listOf("задач", "task", "todo", "напомни", "проверить завтра").any { normalized.contains(it) }
        return hasWeather && (hasNotes || hasTasks) && detectCity(userRequest) != null
    }

    suspend fun run(userRequest: String): McpOrchestrationResult {
        return run(
            userRequest = userRequest,
            demoDelayMs = 0L,
            onLog = {}
        )
    }

    suspend fun run(
        userRequest: String,
        demoDelayMs: Long = 0L,
        onLog: suspend (McpExecutionLogItem) -> Unit
    ): McpOrchestrationResult {
        emitLog(
            onLog = onLog,
            demoDelayMs = demoDelayMs,
            item = McpExecutionLogItem(
                timestamp = currentTimestamp(),
                status = McpExecutionStatus.RUNNING,
                message = "Planning with LLM..."
            )
        )

        val tools = loadToolsFromAllServers()
        val availableToolKeys = tools.map { "${it.serverId}.${it.toolName}" }.toSet()
        val llmPlanResult = buildPipelineWithLlm(userRequest, tools)
        val llmSteps = llmPlanResult.getOrNull().orEmpty()
        val plannedSteps = if (llmPlanResult.isSuccess && llmSteps.isNotEmpty()) {
            emitLog(
                onLog = onLog,
                demoDelayMs = demoDelayMs,
                item = McpExecutionLogItem(
                    timestamp = currentTimestamp(),
                    status = McpExecutionStatus.SUCCESS,
                    message = "LLM planner returned ${llmSteps.size} steps"
                )
            )
            llmSteps
        } else {
            val fallbackSteps = buildFallbackPipeline(userRequest)
            emitLog(
                onLog = onLog,
                demoDelayMs = demoDelayMs,
                item = McpExecutionLogItem(
                    timestamp = currentTimestamp(),
                    status = McpExecutionStatus.INFO,
                    message = "Fallback planner used (${fallbackSteps.size} steps)"
                )
            )
            fallbackSteps
        }

        val steps = if (plannedSteps.isNotEmpty()) plannedSteps else buildFallbackPipeline(userRequest)
        val results = mutableListOf<McpOrchestrationStepResult>()
        var previous = ""

        for ((index, step) in steps.withIndex()) {
            val server = servers.firstOrNull { it.id == step.serverId }
                ?: run {
                    val error = "Unknown MCP serverId: ${step.serverId}"
                    emitLog(
                        onLog = onLog,
                        demoDelayMs = demoDelayMs,
                        item = McpExecutionLogItem(
                            timestamp = currentTimestamp(),
                            status = McpExecutionStatus.ERROR,
                            serverId = step.serverId,
                            toolName = step.toolName,
                            message = error
                        )
                    )
                    return stoppedResult(userRequest, tools, results, error)
                }
            if ("${step.serverId}.${step.toolName}" !in availableToolKeys) {
                val error = "Unknown MCP tool: ${step.serverId}.${step.toolName}"
                emitLog(
                    onLog = onLog,
                    demoDelayMs = demoDelayMs,
                    item = McpExecutionLogItem(
                        timestamp = currentTimestamp(),
                        status = McpExecutionStatus.ERROR,
                        serverId = server.id,
                        serverName = server.name,
                        toolName = step.toolName,
                        message = "[${index + 1}/${steps.size}] ${server.name} → ${step.toolName}\nError: $error"
                    )
                )
                return stoppedResult(userRequest, tools, results, error)
            }

            val resolvedArguments = step.arguments.mapValues { (_, value) ->
                if (value == PREVIOUS_PLACEHOLDER) previous else value
            }

            emitLog(
                onLog = onLog,
                demoDelayMs = demoDelayMs,
                item = McpExecutionLogItem(
                    timestamp = currentTimestamp(),
                    status = McpExecutionStatus.RUNNING,
                    serverId = server.id,
                    serverName = server.name,
                    toolName = step.toolName,
                    message = "[${index + 1}/${steps.size}] ${server.name} → ${step.toolName}"
                )
            )

            val rawResult = mcpAgentRepository.callTool(
                endpoint = server.endpoint,
                name = step.toolName,
                arguments = resolvedArguments
            )
            val toolResult = extractToolText(rawResult)

            results += McpOrchestrationStepResult(
                serverId = server.id,
                serverName = server.name,
                toolName = step.toolName,
                arguments = resolvedArguments,
                result = toolResult
            )

            if (isJsonRpcError(rawResult)) {
                emitLog(
                    onLog = onLog,
                    demoDelayMs = demoDelayMs,
                    item = McpExecutionLogItem(
                        timestamp = currentTimestamp(),
                        status = McpExecutionStatus.ERROR,
                        serverId = server.id,
                        serverName = server.name,
                        toolName = step.toolName,
                        message = "[${index + 1}/${steps.size}] ${server.name} → ${step.toolName}\nError: $toolResult"
                    )
                )
                return stoppedResult(
                    userRequest = userRequest,
                    tools = tools,
                    results = results,
                    error = "MCP orchestration stopped because ${server.id}.${step.toolName} returned an error:\n$toolResult"
                )
            }

            emitLog(
                onLog = onLog,
                demoDelayMs = demoDelayMs,
                item = McpExecutionLogItem(
                    timestamp = currentTimestamp(),
                    status = McpExecutionStatus.SUCCESS,
                    serverId = server.id,
                    serverName = server.name,
                    toolName = step.toolName,
                    message = "[${index + 1}/${steps.size}] ${server.name} → ${step.toolName}"
                )
            )

            previous = toolResult
        }

        emitLog(
            onLog = onLog,
            demoDelayMs = demoDelayMs,
            item = McpExecutionLogItem(
                timestamp = currentTimestamp(),
                status = McpExecutionStatus.SUCCESS,
                message = "Orchestration completed successfully"
            )
        )

        return McpOrchestrationResult(
            userRequest = userRequest,
            servers = servers,
            tools = tools,
            steps = results,
            finalResult = previous
        )
    }

    suspend fun loadToolsFromAllServers(): List<McpToolDescriptor> {
        return servers.flatMap { server ->
            val raw = mcpAgentRepository.listTools(server.endpoint)
            parseToolsList(server, raw)
        }
    }

    fun formatChatAnswer(result: McpOrchestrationResult): String {
        val reportText = result.steps
            .lastOrNull { it.serverId == "weather" && it.toolName == "create_weather_report" }
            ?.result
            ?: result.finalResult
        val noteId = result.steps
            .lastOrNull { it.serverId == "notes" && it.toolName == "save_note" }
            ?.result
            ?.let(::extractJsonId)
        val taskId = result.steps
            .lastOrNull { it.serverId == "tasks" && it.toolName == "create_task" }
            ?.result
            ?.let(::extractJsonId)
        val pipeline = result.steps
            .mapIndexed { index, step -> "${index + 1}. `${step.serverId}.${step.toolName}`" }
            .joinToString("\n")

        return buildString {
            appendLine("## Готово")
            appendLine()
            appendLine("Я получил погоду в Санкт-Петербурге, подготовил отчет, сохранил его в заметки и создал задачу.")
            appendLine()
            appendLine("## Отчет")
            appendLine()
            appendLine(reportText)
            appendLine()
            appendLine("## Результаты")
            appendLine()
            appendLine("- Заметка сохранена: ${noteId ?: "result unavailable"}")
            appendLine("- Задача создана: ${taskId ?: "result unavailable"}")
            appendLine()
            appendLine("## Orchestration pipeline")
            appendLine()
            appendLine(pipeline)
        }.trim()
    }

    fun formatDebugResult(result: McpOrchestrationResult): String {
        return buildString {
            appendLine("Registered MCP servers:")
            result.servers.forEach { server ->
                appendLine("- ${server.id}: ${server.name} (${server.endpoint})")
            }
            appendLine()
            appendLine("Tools:")
            result.tools.forEach { tool ->
                appendLine("- ${tool.serverId}.${tool.toolName}: ${tool.description}")
            }
            appendLine()
            appendLine("Pipeline steps:")
            result.steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. ${step.serverId}.${step.toolName}")
                appendLine("Arguments:")
                appendLine(step.arguments)
                appendLine("Result:")
                appendLine(step.result)
                appendLine()
            }
            appendLine("Final result:")
            appendLine(result.finalResult)
        }
    }

    private fun stoppedResult(
        userRequest: String,
        tools: List<McpToolDescriptor>,
        results: List<McpOrchestrationStepResult>,
        error: String
    ): McpOrchestrationResult {
        return McpOrchestrationResult(
            userRequest = userRequest,
            servers = servers,
            tools = tools,
            steps = results,
            finalResult = error
        )
    }

    private suspend fun buildPipelineWithLlm(
        userRequest: String,
        tools: List<McpToolDescriptor>
    ): Result<List<McpOrchestrationStep>> {
        val toolsText = tools.joinToString("\n") { tool ->
            "- serverId=\"${tool.serverId}\", toolName=\"${tool.toolName}\", description=\"${tool.description}\", inputSchema=${tool.inputSchema}"
        }
        val prompt = """
            Ты MCP orchestration planner.

            У тебя есть несколько MCP-серверов и tools.

            Доступные tools:
            $toolsText

            Пользовательский запрос:
            $userRequest

            Выбери цепочку tools.

            Правила:
            1. Для получения погоды используй serverId="weather", toolName="get_weather_by_city".
            2. Для создания отчета используй serverId="weather", toolName="create_weather_report".
            3. Для сохранения заметки используй serverId="notes", toolName="save_note".
            4. Для создания задачи используй serverId="tasks", toolName="create_task".
            5. Для передачи результата предыдущего шага используй "$PREVIOUS_PLACEHOLDER".
            6. Не выдумывай serverId и toolName.
            7. Верни только JSON-массив без markdown.

            Пример:
            [
              {
                "serverId": "weather",
                "toolName": "get_weather_by_city",
                "arguments": {
                  "city": "Saint Petersburg"
                }
              },
              {
                "serverId": "weather",
                "toolName": "create_weather_report",
                "arguments": {
                  "weatherJson": "$PREVIOUS_PLACEHOLDER"
                }
              },
              {
                "serverId": "notes",
                "toolName": "save_note",
                "arguments": {
                  "title": "Weather report for Saint Petersburg",
                  "content": "$PREVIOUS_PLACEHOLDER"
                }
              },
              {
                "serverId": "tasks",
                "toolName": "create_task",
                "arguments": {
                  "title": "Check weather tomorrow",
                  "description": "$PREVIOUS_PLACEHOLDER",
                  "due": "tomorrow"
                }
              }
            ]
        """.trimIndent()

        return llmClient.sendChat(
            messages = listOf(
                Message(
                    id = "mcp-orchestration-planner",
                    content = prompt,
                    role = MessageRole.USER
                )
            ),
            maxTokens = 900,
            model = null
        ).mapCatching { response ->
            parsePipelineSteps(response.message)
        }
    }

    private fun buildFallbackPipeline(userRequest: String): List<McpOrchestrationStep> {
        val city = detectCity(userRequest) ?: return emptyList()
        val normalized = userRequest.lowercase()
        val wantsReport = listOf("отчет", "отчёт", "сводк", "summary", "report").any { normalized.contains(it) }
        val wantsNotes = listOf("заметк", "notes", "сохрани").any { normalized.contains(it) }
        val wantsTask = listOf("задач", "task", "todo", "напомни", "проверить завтра").any { normalized.contains(it) }

        return buildList {
            add(
                McpOrchestrationStep(
                    serverId = "weather",
                    toolName = "get_weather_by_city",
                    arguments = mapOf("city" to city)
                )
            )
            if (wantsReport || wantsNotes || wantsTask) {
                add(
                    McpOrchestrationStep(
                        serverId = "weather",
                        toolName = "create_weather_report",
                        arguments = mapOf("weatherJson" to PREVIOUS_PLACEHOLDER)
                    )
                )
            }
            if (wantsNotes) {
                add(
                    McpOrchestrationStep(
                        serverId = "notes",
                        toolName = "save_note",
                        arguments = mapOf(
                            "title" to "Weather report for $city",
                            "content" to PREVIOUS_PLACEHOLDER
                        )
                    )
                )
            }
            if (wantsTask) {
                add(
                    McpOrchestrationStep(
                        serverId = "tasks",
                        toolName = "create_task",
                        arguments = mapOf(
                            "title" to "Check weather tomorrow",
                            "description" to PREVIOUS_PLACEHOLDER,
                            "due" to "tomorrow"
                        )
                    )
                )
            }
        }
    }

    private fun parseToolsList(server: McpServerConfig, raw: String): List<McpToolDescriptor> {
        return runCatching {
            val tools = JSONObject(raw)
                .getJSONObject("result")
                .getJSONArray("tools")
            buildList {
                for (index in 0 until tools.length()) {
                    val tool = tools.getJSONObject(index)
                    add(
                        McpToolDescriptor(
                            serverId = server.id,
                            serverName = server.name,
                            endpoint = server.endpoint,
                            toolName = tool.getString("name"),
                            description = tool.optString("description"),
                            inputSchema = tool.optJSONObject("inputSchema")?.toString() ?: "{}"
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun parsePipelineSteps(raw: String): List<McpOrchestrationStep> {
        val json = raw
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val args = item.optJSONObject("arguments") ?: JSONObject()
                add(
                    McpOrchestrationStep(
                        serverId = item.getString("serverId"),
                        toolName = item.getString("toolName"),
                        arguments = args.toMap()
                    )
                )
            }
        }
    }

    private fun extractToolText(raw: String): String {
        return runCatching {
            val json = JSONObject(raw)
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                return@runCatching "MCP error ${error.optInt("code")}: ${error.optString("message")}"
            }
            json.getJSONObject("result")
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        }.getOrDefault(raw)
    }

    private fun isJsonRpcError(raw: String): Boolean {
        return runCatching { JSONObject(raw).has("error") }.getOrDefault(false)
    }

    private fun extractJsonId(text: String): String? {
        return runCatching { JSONObject(text).optString("id").takeIf { it.isNotBlank() } }.getOrNull()
    }

    private fun detectCity(userRequest: String): String? {
        val normalized = userRequest.lowercase()
        return when {
            normalized.contains("санкт-петербург") ||
                normalized.contains("санкт петербург") ||
                normalized.contains("питер") ||
                normalized.contains("spb") -> "Saint Petersburg"
            normalized.contains("москва") || normalized.contains("moscow") -> "Moscow"
            normalized.contains("казань") || normalized.contains("kazan") -> "Kazan"
            normalized.contains("сочи") || normalized.contains("sochi") -> "Sochi"
            else -> null
        }
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        keys().forEach { key ->
            result[key] = get(key)
        }
        return result
    }

    private suspend fun emitLog(
        onLog: suspend (McpExecutionLogItem) -> Unit,
        demoDelayMs: Long,
        item: McpExecutionLogItem
    ) {
        onLog(item)
        if (demoDelayMs > 0L) {
            delay(demoDelayMs)
        }
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private companion object {
        const val PREVIOUS_PLACEHOLDER = "$" + "previous"
    }
}
