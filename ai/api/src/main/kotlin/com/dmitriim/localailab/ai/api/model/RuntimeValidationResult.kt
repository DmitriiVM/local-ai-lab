package com.dmitriim.localailab.ai.api.model

/** Result of runtime-specific model validation. [message] explains a failed validation to the user. */
data class RuntimeValidationResult(val valid: Boolean, val message: String? = null)
