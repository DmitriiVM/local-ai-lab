package com.dmitriim.localailab.feature.models.impl.domain.diagnostics

internal data class ModelCompatibilityIssue(
    val severity: ModelCompatibilityIssueSeverity,
    val message: String,
)

internal enum class ModelCompatibilityIssueSeverity {
    BLOCKING,
    ADVISORY,
}
