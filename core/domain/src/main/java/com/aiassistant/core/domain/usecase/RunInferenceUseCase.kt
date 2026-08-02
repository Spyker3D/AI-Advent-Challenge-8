package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.inference.InferenceEngine
import com.aiassistant.core.domain.inference.InferenceExecutionResult
import com.aiassistant.core.domain.inference.InferenceMode
import javax.inject.Inject

class RunInferenceUseCase @Inject constructor(
    private val inferenceEngine: InferenceEngine
) {
    suspend operator fun invoke(input: String, mode: InferenceMode): Result<InferenceExecutionResult> =
        inferenceEngine.execute(input, mode)
}
