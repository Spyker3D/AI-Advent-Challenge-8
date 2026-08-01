package com.aiassistant.core.data.client

import com.aiassistant.core.domain.entity.ChatSettings

internal fun selectOllamaModel(override: String?, settingsModel: String): String =
    override?.trim()?.takeIf { it.isNotEmpty() }
        ?: settingsModel.trim().takeIf { it.isNotEmpty() }
        ?: ChatSettings.DEFAULT_LOCAL_MODEL
