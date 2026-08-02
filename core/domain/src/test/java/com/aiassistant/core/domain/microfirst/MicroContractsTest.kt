package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.inference.IncidentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroContractsTest {
    @Test fun `fallback prompt mirrors cause first runner contract`() {
        val prompt = MicroFallbackContract.PROMPT
        IncidentCategory.entries.forEach { category -> assertTrue(prompt.contains(category.name)) }
        assertTrue(prompt.contains("retry later"))
        assertTrue(prompt.contains("does not determine the category"))
        assertTrue(prompt.contains("only category, confidence, and reason"))
        assertTrue(prompt.contains("Do not add title, message, user_action"))
    }

    @Test fun `formatter deterministically supports every fallback category`() {
        IncidentCategory.entries.forEach { category ->
            val first = MicroResponseFormatter.format(category)
            assertEquals(first, MicroResponseFormatter.format(category))
            assertTrue(first.isNotBlank())
        }
    }

    @Test fun `fallback accepts exact three field response`() {
        assertSuccess(validJson(), IncidentCategory.NETWORK_UNAVAILABLE)
    }

    @Test fun `fallback rejects missing extra and invalid structural fields`() {
        assertFailure("not json")
        assertFailure("[]")
        assertFailure("""{"category":"NETWORK_UNAVAILABLE","confidence":0.9}""")
        assertFailure("""{"category":"NETWORK_UNAVAILABLE","confidence":0.9,"reason":"Нет сети","title":"Лишнее поле"}""")
        assertFailure(validJson().replace("0.9", "1.1"))
        assertFailure(validJson().replace("Нет сети", "No network"))
        assertFailure(validJson().replace("Нет сети", ""))
    }

    @Test fun `rate count wording is structurally accepted`() {
        assertSuccess(
            """{"category":"OPENAI_RATE_LIMIT","confidence":0.8,"reason":"Сервис ограничил количество запросов"}""",
            IncidentCategory.OPENAI_RATE_LIMIT
        )
        assertSuccess(
            """{"category":"OPENAI_RATE_LIMIT","confidence":0.8,"reason":"Превышено ограничение по количеству запросов"}""",
            IncidentCategory.OPENAI_RATE_LIMIT
        )
    }

    @Test fun `timeout category is not reclassified from rate prose`() {
        assertSuccess(
            """{"category":"OPENAI_TIMEOUT","confidence":0.8,"reason":"Превышен лимит запросов и истекло время ожидания"}""",
            IncidentCategory.OPENAI_TIMEOUT
        )
    }

    @Test fun `presentation paraphrases are outside fallback contract`() {
        assertFailure(
            """{"category":"OPENAI_RATE_LIMIT","confidence":0.8,"reason":"Лимит запросов","title":"Любой заголовок"}"""
        )
    }

    private fun assertSuccess(raw: String, category: IncidentCategory) {
        val parsed = MicroFallbackContract.parse(raw)
        assertTrue((parsed as? FallbackParseResult.Failure)?.error ?: "expected success", parsed is FallbackParseResult.Success)
        assertEquals(category, (parsed as FallbackParseResult.Success).value.category)
    }

    private fun assertFailure(raw: String) {
        assertTrue(MicroFallbackContract.parse(raw) is FallbackParseResult.Failure)
    }

    private fun validJson() = """{"category":"NETWORK_UNAVAILABLE","confidence":0.9,"reason":"Нет сети"}"""
}
