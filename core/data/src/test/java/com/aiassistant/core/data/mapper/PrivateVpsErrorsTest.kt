package com.aiassistant.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateVpsErrorsTest {

    @Test
    fun `HTTP 429 includes Retry-After when present`() {
        val message = privateVpsHttpError(code = 429, retryAfter = "30")

        assertEquals(
            "Превышен rate limit приватного сервиса. Повторите через 30 секунд.",
            message
        )
    }

    @Test
    fun `HTTP 429 uses clear fallback without Retry-After`() {
        val message = privateVpsHttpError(code = 429)

        assertEquals(
            "Превышен rate limit приватного сервиса. Повторите позже.",
            message
        )
    }

    @Test
    fun `other HTTP codes are not classified as rate limit`() {
        val clientError = privateVpsHttpError(code = 400, retryAfter = "30")
        val serverError = privateVpsHttpError(code = 503, retryAfter = "30")

        assertFalse(clientError.contains("rate limit", ignoreCase = true))
        assertFalse(serverError.contains("rate limit", ignoreCase = true))
        assertTrue(serverError.contains("недоступен", ignoreCase = true))
    }
}