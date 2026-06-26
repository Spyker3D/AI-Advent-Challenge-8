package com.aiassistant.core.data.mcp

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
    fun listTools(): String = postJson(
        """
        {
          "jsonrpc": "2.0",
          "id": 1,
          "method": "tools/list",
          "params": {}
        }
        """.trimIndent()
    )

    fun callGetTaskStatus(taskId: String): String = postJson(
        """
        {
          "jsonrpc": "2.0",
          "id": 2,
          "method": "tools/call",
          "params": {
            "name": "get_task_status",
            "arguments": {
              "taskId": "${taskId.escapeJson()}"
            }
          }
        }
        """.trimIndent()
    )

    private fun postJson(json: String): String = try {
        val request = Request.Builder()
            .url(MCP_ENDPOINT)
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
        "Ошибка MCP-запроса:\n${throwable.stackTraceString()}"
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

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