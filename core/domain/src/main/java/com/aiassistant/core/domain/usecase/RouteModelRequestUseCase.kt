package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.routing.ModelRouter
import com.aiassistant.core.domain.routing.RoutingResult
import javax.inject.Inject

/** Requires LOCAL_OLLAMA to be the active provider. No ordinary chat path invokes this opt-in use case. */
class RouteModelRequestUseCase @Inject constructor(private val modelRouter: ModelRouter) {
    suspend operator fun invoke(userText: String): Result<RoutingResult> = modelRouter.route(userText)
}
