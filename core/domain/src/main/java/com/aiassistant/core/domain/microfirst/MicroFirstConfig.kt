package com.aiassistant.core.domain.microfirst

object MicroFirstConfig {
    const val MICRO_MODEL = "nomic-embed-text:latest"
    const val FALLBACK_MODEL = "qwen2.5:7b-instruct"
    const val MIN_TOP_SCORE = 0.70
    const val MIN_MARGIN = 0.06
}
