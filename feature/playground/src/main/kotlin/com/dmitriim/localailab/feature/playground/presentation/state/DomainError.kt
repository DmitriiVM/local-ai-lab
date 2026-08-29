package com.dmitriim.localailab.feature.playground.presentation.state

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelId

data class DomainError(
    val title: String,
    val explanation: String,
    val suggestedAction: String? = null,
    val technicalDetails: String? = null,
    val engineId: EngineId? = null,
    val modelId: ModelId? = null,
    val retryable: Boolean = false,
)
