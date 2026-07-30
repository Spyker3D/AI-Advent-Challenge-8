package com.aiassistant.core.data.client

import com.aiassistant.core.domain.entity.ChatSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class OllamaModelSelectorTest {
    @Test fun `nonblank override wins exactly`() { assertEquals("llama3.2:3b", selectOllamaModel(" llama3.2:3b ", "qwen2.5:7b-instruct")) }
    @Test fun `blank override uses setting`() { assertEquals("custom:model", selectOllamaModel(" ", "custom:model")) }
    @Test fun `blank values use default`() { assertEquals(ChatSettings.DEFAULT_LOCAL_MODEL, selectOllamaModel(null, " ")) }
}
