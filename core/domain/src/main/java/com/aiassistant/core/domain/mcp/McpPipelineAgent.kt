package com.aiassistant.core.domain.mcp

import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpPipelineAgent @Inject constructor(
    private val mcpAgentRepository: McpAgentRepository,
    private val llmClient: LlmClient
) {
    suspend fun run(userRequest: String): McpPipelineResult {
        val toolsList = runCatching { mcpAgentRepository.listTools() }.getOrDefault("")
        val steps = buildPipelineWithLlm(userRequest, toolsList)
            .getOrElse { buildFallbackPipeline(userRequest) }
            .ifEmpty { buildFallbackPipeline(userRequest) }

        val results = mutableListOf<McpPipelineStepResult>()
        var previous = ""

        for (step in steps) {
            val resolvedArguments = step.arguments.mapValues { (_, value) ->
                if (value == PREVIOUS_PLACEHOLDER) previous else value
            }
            val rawResult = mcpAgentRepository.callTool(step.toolName, resolvedArguments)
            val toolResult = extractToolText(rawResult)

            results += McpPipelineStepResult(
                toolName = step.toolName,
                arguments = resolvedArguments,
                result = toolResult
            )

            if (isJsonRpcError(rawResult)) {
                return McpPipelineResult(
                    userRequest = userRequest,
                    steps = results,
                    finalResult = "MCP pipeline stopped because tool ${step.toolName} returned an error:\n$toolResult"
                )
            }

            previous = toolResult
        }

        return McpPipelineResult(
            userRequest = userRequest,
            steps = results,
            finalResult = previous
        )
    }

    fun canHandleWeatherPipeline(userRequest: String): Boolean {
        val normalized = userRequest.lowercase()
        val hasWeatherIntent = listOf(
            "погода",
            "погоде",
            "температура",
            "температуру",
            "weather",
            "temperature"
        ).any { normalized.contains(it) }
        return hasWeatherIntent && detectCity(userRequest) != null
    }

    fun formatChatAnswer(result: McpPipelineResult): String {
        val pipeline = result.steps
            .mapIndexed { index, step -> "${index + 1}. ${step.toolName}" }
            .joinToString("\n")
        val finalText = result.finalResult
        val reportUrl = extractReportUrl(finalText)

        return if (reportUrl != null) {
            """
            Готово. Я получил погоду, подготовил отчет и сохранил его в файл.

            Файл отчета:
            $reportUrl

            Pipeline:
            $pipeline
            """.trimIndent()
        } else if (result.steps.size == 1 && result.steps.first().toolName == "get_weather_by_city") {
            """
            ${formatWeatherJsonForChat(finalText)}

            Pipeline:
            $pipeline
            """.trimIndent()
        } else {
            """
            $finalText

            Pipeline:
            $pipeline
            """.trimIndent()
        }
    }

    fun formatDebugResult(result: McpPipelineResult): String {
        return buildString {
            appendLine("User request:")
            appendLine(result.userRequest)
            appendLine()
            appendLine("Pipeline steps:")
            result.steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. ${step.toolName}")
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

    private suspend fun buildPipelineWithLlm(
        userRequest: String,
        toolsList: String
    ): Result<List<McpPipelineStep>> {
        val prompt = """
            Ты MCP pipeline planner.

            У тебя есть MCP tools:
            - get_weather_by_city(city)
            - create_weather_report(weatherJson)
            - save_report_to_file(fileName, content)

            tools/list:
            $toolsList

            Пользовательский запрос:
            $userRequest

            Выбери цепочку tools.

            Правила:
            1. Если пользователь спрашивает погоду или температуру — вызови get_weather_by_city.
            2. Если пользователь просит отчет, сводку или summary — после get_weather_by_city вызови create_weather_report.
            3. Если пользователь просит сохранить, записать в файл или заметку — после create_weather_report вызови save_report_to_file.
            4. Если пользователь пишет "подготовь отчет", это означает create_weather_report + save_report_to_file.
            5. Для передачи результата предыдущего шага используй "$PREVIOUS_PLACEHOLDER".
            6. Верни только JSON-массив шагов без markdown.

            Формат:
            [
              {
                "toolName": "get_weather_by_city",
                "arguments": {
                  "city": "Saint Petersburg"
                }
              },
              {
                "toolName": "create_weather_report",
                "arguments": {
                  "weatherJson": "$PREVIOUS_PLACEHOLDER"
                }
              },
              {
                "toolName": "save_report_to_file",
                "arguments": {
                  "fileName": "weather-report-saint-petersburg.txt",
                  "content": "$PREVIOUS_PLACEHOLDER"
                }
              }
            ]
        """.trimIndent()

        return llmClient.sendChat(
            messages = listOf(
                Message(
                    id = "mcp-pipeline-planner",
                    content = prompt,
                    role = MessageRole.USER
                )
            ),
            maxTokens = 700,
            model = null
        ).mapCatching { response ->
            parsePipelineSteps(response.message)
        }
    }

    private fun buildFallbackPipeline(userRequest: String): List<McpPipelineStep> {
        val city = detectCity(userRequest) ?: return emptyList()
        val normalized = userRequest.lowercase()
        val wantsReport = listOf("отчет", "отчёт", "сводк", "summary", "report").any { normalized.contains(it) }
        val wantsSave = wantsReport || listOf("сохрани", "сохранить", "файл", "запиши", "заметк").any {
            normalized.contains(it)
        }

        return buildList {
            add(
                McpPipelineStep(
                    toolName = "get_weather_by_city",
                    arguments = mapOf("city" to city)
                )
            )
            if (wantsReport) {
                add(
                    McpPipelineStep(
                        toolName = "create_weather_report",
                        arguments = mapOf("weatherJson" to PREVIOUS_PLACEHOLDER)
                    )
                )
            }
            if (wantsSave) {
                add(
                    McpPipelineStep(
                        toolName = "save_report_to_file",
                        arguments = mapOf(
                            "fileName" to "weather-report-${city.toSlug()}.txt",
                            "content" to PREVIOUS_PLACEHOLDER
                        )
                    )
                )
            }
        }
    }

    private fun parsePipelineSteps(raw: String): List<McpPipelineStep> {
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
                    McpPipelineStep(
                        toolName = item.getString("toolName"),
                        arguments = args.toMap()
                    )
                )
            }
        }.filter { step ->
            step.toolName in setOf(
                "get_weather_by_city",
                "create_weather_report",
                "save_report_to_file"
            )
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

    private fun extractReportUrl(text: String): String? {
        return Regex("http://31\\.129\\.110\\.10:3000/reports/[^\\s\"}]+")
            .find(text)
            ?.value
    }

    private fun formatWeatherJsonForChat(text: String): String {
        return runCatching {
            val json = JSONObject(text)
            """
            Сейчас в ${json.getString("city")} ${json.getDouble("temperature")}°C.
            Ветер: ${json.getDouble("windSpeed")} km/h.
            Осадки: ${json.getDouble("precipitation")} mm.
            Источник: ${json.getString("source")}.
            Обновлено: ${json.getString("timestamp")}.
            """.trimIndent()
        }.getOrDefault(text)
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        keys().forEach { key ->
            result[key] = get(key)
        }
        return result
    }

    private fun String.toSlug(): String {
        return lowercase()
            .replace("saint petersburg", "saint-petersburg")
            .replace(" ", "-")
            .replace(Regex("[^a-z0-9-]"), "")
            .ifBlank { "weather" }
    }

    private companion object {
        const val PREVIOUS_PLACEHOLDER = "$" + "previous"
    }
}
