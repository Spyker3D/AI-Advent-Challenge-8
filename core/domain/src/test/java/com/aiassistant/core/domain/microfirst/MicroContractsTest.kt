package com.aiassistant.core.domain.microfirst

import com.aiassistant.core.domain.inference.IncidentAction
import com.aiassistant.core.domain.inference.IncidentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroContractsTest {
    @Test fun `formatter has exact text for every concrete category and no enum leakage`() {
        val expected = mapOf(
            IncidentCategory.NETWORK_UNAVAILABLE to "Нет подключения к сети\n\nПроверьте интернет-соединение.\n\nПроверить подключение",
            IncidentCategory.OPENAI_RATE_LIMIT to "Превышен лимит запросов\n\nПодождите немного и повторите запрос.\n\nПовторить позже",
            IncidentCategory.OPENAI_TIMEOUT to "Время ожидания истекло\n\nОтвет не был получен вовремя.\n\nПовторить запрос",
            IncidentCategory.EMPTY_AI_RESPONSE to "Получен пустой ответ\n\nМодель не вернула содержимое.\n\nПовторить запрос",
            IncidentCategory.LOCAL_HISTORY_UNAVAILABLE to "История чатов недоступна\n\nНе удалось загрузить локальную историю.\n\nПерезагрузить историю"
        )
        expected.forEach { (category, text) ->
            val actual = MicroResponseFormatter.format(category)
            assertEquals(text, actual)
            assertTrue(IncidentCategory.entries.none { actual!!.contains(it.name) })
            assertTrue(IncidentAction.entries.none { actual!!.contains(it.name) })
        }
        assertNull(MicroResponseFormatter.format(IncidentCategory.AMBIGUOUS))
    }

    @Test fun `fallback accepts strict valid response`() {
        assertTrue(MicroFallbackContract.parse(validJson()) is FallbackParseResult.Success)
    }

    @Test fun `fallback rejects confidence outside range and non Russian fields`() {
        assertValidationFailure(validJson().replace("0.9", "1.1"))
        assertValidationFailure(validJson().replace("Нет сети", "No network"))
    }

    @Test fun `fallback rejects category and action enum leakage in user action`() {
        assertValidationFailure(validJson().replace("Проверить подключение", "NETWORK_UNAVAILABLE проверить"))
        assertValidationFailure(validJson().replace("Проверить подключение", "RETRY_REQUEST повторить"))
    }

    @Test fun `invalid JSON and non object are correction eligible`() {
        assertValidationFailure("not json")
        assertValidationFailure("[]")
    }

    private fun assertValidationFailure(raw: String) {
        val failure = MicroFallbackContract.parse(raw) as FallbackParseResult.Failure
        assertTrue(failure.validationFailure)
    }

    private fun validJson() = """{"category":"NETWORK_UNAVAILABLE","confidence":0.9,"title":"Нет сети","message":"Проверьте сеть","user_action":"Проверить подключение"}"""
}
