package com.aiassistant.developer.mcp

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class GitBranchResult(val branch: String?, val connected: Boolean, val message: String?)

interface GitBranchProvider { fun currentBranch(): GitBranchResult }
interface GitReviewProvider {
    fun changedFiles(baseRef: String, headRef: String): String
    fun diff(baseRef: String, headRef: String): String
}

class GitMcpClient(private val endpoint: String) : GitBranchProvider, GitReviewProvider {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    override fun currentBranch(): GitBranchResult = try {
        GitBranchResult(call("get_current_git_branch", "{}").trim(), true, null)
    } catch (error: Exception) {
        GitBranchResult(null, false, "Git branch is unavailable because the MCP server is not running: ${error.message}")
    }

    override fun changedFiles(baseRef: String, headRef: String): String =
        call("get_changed_files", refs(baseRef, headRef))

    override fun diff(baseRef: String, headRef: String): String =
        call("get_git_diff", refs(baseRef, headRef))

    private fun refs(baseRef: String, headRef: String): String =
        com.google.gson.Gson().toJson(mapOf("baseRef" to baseRef, "headRef" to headRef))

    private fun call(tool: String, argumentsJson: String): String {
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$tool","arguments":$argumentsJson}}"""
        val request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) { "MCP returned HTTP ${response.statusCode()}" }
        val json = JsonParser.parseString(response.body()).asJsonObject
        if (json.has("error")) throw IllegalStateException(json.getAsJsonObject("error").get("message").asString)
        return json.getAsJsonObject("result").getAsJsonArray("content")[0].asJsonObject.get("text").asString
    }
}
