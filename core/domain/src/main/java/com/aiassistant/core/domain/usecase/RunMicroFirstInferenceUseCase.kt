package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.microfirst.MicroFirstInferenceEngine
import com.aiassistant.core.domain.microfirst.MicroFirstResult
import javax.inject.Inject

class RunMicroFirstInferenceUseCase @Inject constructor(
    private val engine: MicroFirstInferenceEngine
) {
    suspend operator fun invoke(input: String): Result<MicroFirstResult> = engine.execute(input)
}
