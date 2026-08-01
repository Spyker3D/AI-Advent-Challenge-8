package com.aiassistant.core.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextStrategyTest {
    @Test fun `legacy no strategy migrates to full history`() {
        assertEquals(ContextStrategy.FULL_HISTORY, ContextStrategy.fromStoredValue("NO_STRATEGY"))
    }
    @Test fun `none remains distinct from full history`() {
        assertEquals(ContextStrategy.NONE, ContextStrategy.fromStoredValue("NONE"))
    }
}
