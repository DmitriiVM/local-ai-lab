package com.dmitriim.localailab.feature.models.impl.domain.diagnostics

import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelCompatibility
import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelCompatibilityState

/** Classifies structured model-compatibility issues without reading device or runtime state. */
internal object ModelCompatibilityPolicy {
    fun evaluate(issues: List<ModelCompatibilityIssue>): ModelCompatibility = when {
        issues.any { it.severity == ModelCompatibilityIssueSeverity.BLOCKING } ->
            ModelCompatibility(ModelCompatibilityState.INCOMPATIBLE, issues.map { it.message })

        issues.isNotEmpty() ->
            ModelCompatibility(ModelCompatibilityState.ADVISORY_WARNING, issues.map { it.message })

        else -> ModelCompatibility(
            state = ModelCompatibilityState.COMPATIBLE,
            reasons = listOf("Compatible with the installed CPU runtime."),
        )
    }
}
