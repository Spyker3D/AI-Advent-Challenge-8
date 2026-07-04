package com.aiassistant.core.data.mcp

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()

    fun listTools(): String = listTools(MCP_ENDPOINT)

    fun listTools(endpoint: String): String = postJson(
        endpoint = endpoint,
        json = gson.toJson(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to 1,
                "method" to "tools/list",
                "params" to emptyMap<String, Any?>()
            )
        )
    )

    fun callTool(name: String, arguments: Map<String, Any?>): String = callTool(
        endpoint = MCP_ENDPOINT,
        name = name,
        arguments = arguments
    )

    fun callTool(endpoint: String, name: String, arguments: Map<String, Any?>): String = postJson(
        endpoint = endpoint,
        json = gson.toJson(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to 100,
                "method" to "tools/call",
                "params" to mapOf(
                    "name" to name,
                    "arguments" to arguments
                )
            )
        )
    )

    fun callGetTaskStatus(taskId: String): String = callTool(
        name = "get_task_status",
        arguments = mapOf("taskId" to taskId)
    )

    fun callGetWeatherSummary(limit: Int): String = callTool(
        name = "get_weather_summary",
        arguments = mapOf("limit" to limit)
    )

    fun callGetWeatherHistory(limit: Int): String = callTool(
        name = "get_weather_history",
        arguments = mapOf("limit" to limit)
    )

    fun callCollectWeatherNow(): String = callTool(
        name = "collect_weather_now",
        arguments = emptyMap()
    )

    private fun postJson(endpoint: String, json: String): String = try {
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json, text/event-stream")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                body
            } else {
                "HTTP ${response.code} ${response.message}\n$body"
            }
        }
    } catch (throwable: Throwable) {
        "MCP request error:\n${throwable.stackTraceString()}"
    }

    private fun Throwable.stackTraceString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private companion object {
        const val MCP_ENDPOINT = "http://31.129.110.10:3000/mcp"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
