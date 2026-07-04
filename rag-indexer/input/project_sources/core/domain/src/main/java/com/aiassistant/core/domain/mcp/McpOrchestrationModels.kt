package com.aiassistant.core.domain.mcp

data class McpServerConfig(
    val id: String,
    val name: String,
    val endpoint: String
)

data class McpToolDescriptor(
    val serverId: String,
    val serverName: String,
    val endpoint: String,
    val toolName: String,
    val description: String,
    val inputSchema: String
)

data class McpOrchestrationStep(
    val serverId: String,
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap()
)

data class McpOrchestrationStepResult(
    val serverId: String,
    val serverName: String,
    val toolName: String,
    val arguments: Map<String, Any?>,
    val result: String
)

data class McpOrchestrationResult(
    val userRequest: String,
    val servers: List<McpServerConfig>,
    val tools: List<McpToolDescriptor>,
    val steps: List<McpOrchestrationStepResult>,
    val finalResult: String
)

data class McpExecutionLogItem(
    val timestamp: String,
    val status: McpExecutionStatus,
    val serverId: String? = null,
    val serverName: String? = null,
    val toolName: String? = null,
    val message: String
)

enum class McpExecutionStatus {
    INFO,
    RUNNING,
    SUCCESS,
    ERROR
}

object McpServerRegistry {
    val mcpServers = listOf(
        McpServerConfig(
            id = "weather",
            name = "Weather MCP",
            endpoint = "http://31.129.110.10:3000/mcp"
        ),
        McpServerConfig(
            id = "notes",
            name = "Notes MCP",
            endpoint = "http://31.129.110.10:3001/mcp"
        ),
        McpServerConfig(
            id = "tasks",
            name = "Tasks MCP",
            endpoint = "http://31.129.110.10:3002/mcp"
        )
    )
}
