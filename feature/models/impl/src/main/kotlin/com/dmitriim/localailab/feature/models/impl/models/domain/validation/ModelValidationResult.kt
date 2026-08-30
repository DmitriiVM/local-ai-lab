package com.dmitriim.localailab.feature.models.impl.models.domain.validation

import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState

data class ModelValidationResult(
    val state: ModelValidationState,
    val message: String? = null,
)
