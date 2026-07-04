package com.aiassistant.core.domain.invariant

import android.util.Log
import javax.inject.Inject

sealed class InvariantValidationResult {
    data class Pass(val response: String) : InvariantValidationResult()

    data class Fail(
        val violations: List<String>,
        val originalResponse: String
    ) : InvariantValidationResult()
}

class InvariantValidator @Inject constructor() {
    fun validateResponse(
        response: String,
        invariants: List<Invariant>
    ): InvariantValidationResult {
        debugLog("response=${response.take(1000)}")
        val checks = invariants.map { invariant ->
            val passed = invariant.check(response)
            debugLog(
                "id=${invariant.id}, passed=$passed, description=${invariant.description}"
            )
            invariant to passed
        }
        val violations = checks
            .filterNot { (_, passed) -> passed }
            .map { (invariant, _) -> invariant.violationMessage() }

        return if (violations.isEmpty()) {
            InvariantValidationResult.Pass(response)
        } else {
            InvariantValidationResult.Fail(violations, response)
        }
    }

    private fun debugLog(message: String) {
        runCatching { Log.d("INVARIANTS", message) }
    }
}
