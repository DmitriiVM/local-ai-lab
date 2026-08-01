package com.dmitriim.localaiplayground.core.model.library

enum class ModelCompatibilityState {
    COMPATIBLE,
    INCOMPATIBLE,
    ADVISORY_WARNING,
}

data class ModelCompatibility(
    val state: ModelCompatibilityState,
    val reasons: List<String>,
)
