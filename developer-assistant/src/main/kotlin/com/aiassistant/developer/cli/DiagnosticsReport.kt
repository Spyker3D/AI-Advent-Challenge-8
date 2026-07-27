package com.aiassistant.developer.cli

import com.aiassistant.developer.config.AssistantConfig

object DiagnosticsReport {
    fun create(config: AssistantConfig): String = """Project root: ${config.projectRoot}
Generation model: ${config.openAiModel}
OpenAI API key: ${if (config.openAiApiKey.isBlank()) "not configured" else "configured"}
Tool iteration limit: ${config.maxToolIterations}
Mode: ${if (config.dryRun) "dry-run" else "interactive"}"""
}