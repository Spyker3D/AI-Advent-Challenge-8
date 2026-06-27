package com.aiassistant.core.domain.mcp

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import org.junit.Assert.assertTrue
import org.junit.Test

class McpPipelineAgentTest {
    private val agent = McpPipelineAgent(
        mcpAgentRepository = FakeMcpAgentRepository(),
        llmClient = FakeLlmClient()
    )

    @Test
    fun `format chat answer includes report text and saved file url`() {
        val report = weatherReport()
        val answer = agent.formatChatAnswer(
            McpPipelineResult(
                userRequest = "Prepare weather report",
                steps = listOf(
                    McpPipelineStepResult(
                        toolName = "get_weather_by_city",
                        arguments = mapOf("city" to "Saint Petersburg"),
                        result = weatherJson()
                    ),
                    McpPipelineStepResult(
                        toolName = "create_weather_report",
                        arguments = emptyMap(),
                        result = report
                    ),
                    McpPipelineStepResult(
                        toolName = "save_report_to_file",
                        arguments = emptyMap(),
                        result = """
                            {
                              "message": "Report saved successfully.",
                              "url": "http://31.129.110.10:3000/reports/weather-report-saint-petersburg.txt"
                            }
                        """.trimIndent()
                    )
                ),
                finalResult = """
                    {
                      "message": "Report saved successfully.",
                      "url": "http://31.129.110.10:3000/reports/weather-report-saint-petersburg.txt"
                    }
                """.trimIndent()
            )
        )

        assertTrue(answer.contains("###"))
        assertTrue(answer.contains(report))
        assertTrue(answer.contains("[ Открыть отчет](http://31.129.110.10:3000/reports/weather-report-saint-petersburg.txt)"))
        assertTrue(answer.contains("1. `get_weather_by_city`\n2. `create_weather_report`\n3. `save_report_to_file`"))
        assertTrue(answer.indexOf(report) < answer.indexOf("http://31.129.110.10:3000/reports/weather-report-saint-petersburg.txt"))
    }

    @Test
    fun `format chat answer includes report text when pipeline has no save step`() {
        val report = weatherReport()
        val answer = agent.formatChatAnswer(
            McpPipelineResult(
                userRequest = "Weather in Saint Petersburg",
                steps = listOf(
                    McpPipelineStepResult(
                        toolName = "get_weather_by_city",
                        arguments = mapOf("city" to "Saint Petersburg"),
                        result = weatherJson()
                    ),
                    McpPipelineStepResult(
                        toolName = "create_weather_report",
                        arguments = emptyMap(),
                        result = report
                    )
                ),
                finalResult = report
            )
        )

        assertTrue(answer.contains(report))
        assertTrue(answer.contains("### Pipeline"))
        assertTrue(answer.contains("1. `get_weather_by_city`\n2. `create_weather_report`"))
    }

    private fun weatherJson(): String {
        return """
            {
              "city": "Saint Petersburg",
              "timestamp": "2026-06-27T08:40:00.000Z",
              "temperature": 18.1,
              "windSpeed": 5.4,
              "precipitation": 0,
              "source": "Open-Meteo"
            }
        """.trimIndent()
    }

    private fun weatherReport(): String {
        return """
            Weather Report

            City: Saint Petersburg
            Temperature: 18.1C
            Wind speed: 5.4 km/h
            Precipitation: 0 mm
            Source: Open-Meteo
            Updated at: 2026-06-27T08:40:00.000Z

            Summary:
            Current weather in Saint Petersburg is 18.1C with wind speed 5.4 km/h and precipitation 0 mm.
        """.trimIndent()
    }
}

private class FakeMcpAgentRepository : McpAgentRepository {
    override suspend fun listTools(): String = ""
    override suspend fun callTool(name: String, arguments: Map<String, Any?>): String = ""
    override suspend fun checkTaskStatus(taskId: String): String = ""
    override suspend fun getWeatherSummary(limit: Int): String = ""
    override suspend fun getWeatherHistory(limit: Int): String = ""
    override suspend fun collectWeatherNow(): String = ""
}

private class FakeLlmClient : LlmClient {
    override suspend fun sendChat(
        messages: List<Message>,
        maxTokens: Int?,
        model: String?
    ): Result<ChatResponse> = Result.success(ChatResponse(""))
}
