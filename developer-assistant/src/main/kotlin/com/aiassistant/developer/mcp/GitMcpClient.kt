package com.aiassistant.developer.mcp

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class GitBranchResult(val branch: String?, val connected: Boolean, val message: String?)

interface GitBranchProvider { fun currentBranch(): GitBranchResult }

class GitMcpClient(private val endpoint: String) : GitBranchProvider {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    override fun currentBranch(): GitBranchResult = try {
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_current_git_branch","arguments":{}}}"""
        val request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299)
        val json = JsonParser.parseString(response.body()).asJsonObject
        if (json.has("error")) GitBranchResult(null, true, json.getAsJsonObject("error").get("message").asString)
        else GitBranchResult(json.getAsJsonObject("result").getAsJsonArray("content")[0].asJsonObject.get("text").asString.trim(), true, null)
    } catch (_: Exception) {
        GitBranchResult(null, false, "Git branch is unavailable because the MCP server is not running.")
    }
}
