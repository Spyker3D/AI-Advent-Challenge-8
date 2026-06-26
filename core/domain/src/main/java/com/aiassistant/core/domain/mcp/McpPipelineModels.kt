package com.aiassistant.core.domain.mcp

data class McpPipelineStep(
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap()
)

data class McpPipelineStepResult(
    val toolName: String,
    val arguments: Map<String, Any?>,
    val result: String
)

data class McpPipelineResult(
    val userRequest: String,
    val steps: List<McpPipelineStepResult>,
    val finalResult: String
)
