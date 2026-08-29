package com.dmitriim.localailab.feature.models.api.domain.diagnostics

enum class ModelCompatibilityState {
    COMPATIBLE,
    INCOMPATIBLE,
    ADVISORY_WARNING,
}

data class ModelCompatibility(val state: ModelCompatibilityState, val reasons: List<String>)
