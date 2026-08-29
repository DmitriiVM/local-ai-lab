package com.dmitriim.localailab.feature.playground.impl.presentation

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.capability.CapabilityReadiness
import com.dmitriim.localailab.ai.api.capability.CapabilityReadinessState
import com.dmitriim.localailab.ai.api.engine.EngineAvailability
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState

internal fun buildCapabilityReadiness(
    availability: List<EngineAvailability>,
    installed: List<InstalledModel> = emptyList(),
): List<CapabilityReadiness> = AiCapability.entries.map { capability ->
    val relevant = when (capability) {
        AiCapability.VOICE_ASSISTANT -> availability.filter { result ->
            result.descriptor.capabilities.any {
                it == AiCapability.CHAT ||
                    it == AiCapability.SPEECH_TO_TEXT ||
                    it == AiCapability.TEXT_TO_SPEECH
            }
        }
        else -> availability.filter { capability in it.descriptor.capabilities }
    }
    val requiredCapabilities = when (capability) {
        AiCapability.VOICE_ASSISTANT -> setOf(
            AiCapability.CHAT,
            AiCapability.SPEECH_TO_TEXT,
            AiCapability.TEXT_TO_SPEECH,
        )
        else -> setOf(capability)
    }
    val availableCapabilities = relevant
        .filterIsInstance<EngineAvailability.Available>()
        .flatMapTo(mutableSetOf()) { it.descriptor.capabilities }

    when {
        relevant.isEmpty() ||
            !availableCapabilities.containsAll(requiredCapabilities) &&
            relevant.all { it is EngineAvailability.Unsupported } ->
            CapabilityReadiness(
                capability = capability,
                state = CapabilityReadinessState.UNSUPPORTED,
                explanation = relevant
                    .filterIsInstance<EngineAvailability.Unsupported>()
                    .joinToString(" ") { it.reason }
                    .ifBlank { "No compatible bundled engine is available." },
                engines = relevant,
            )
        relevant.any { it is EngineAvailability.TemporarilyUnavailable } &&
            !availableCapabilities.containsAll(requiredCapabilities) ->
            CapabilityReadiness(
                capability = capability,
                state = CapabilityReadinessState.TEMPORARILY_UNAVAILABLE,
                explanation = relevant
                    .filterIsInstance<EngineAvailability.TemporarilyUnavailable>()
                    .joinToString(" ") { it.reason },
                engines = relevant,
            )
        else -> {
            val installedCapabilities = installed
                .filter { it.validationState == ModelValidationState.READY }
                .flatMapTo(mutableSetOf()) { it.manifest.capabilities }
            if (installedCapabilities.containsAll(requiredCapabilities)) {
                CapabilityReadiness(
                    capability = capability,
                    state = CapabilityReadinessState.READY,
                    explanation = "A compatible validated local model is installed.",
                    engines = relevant,
                )
            } else {
                CapabilityReadiness(
                    capability = capability,
                    state = CapabilityReadinessState.MODEL_REQUIRED,
                    explanation = "The local runtime is available. Import a compatible model in Models.",
                    engines = relevant,
                )
            }
        }
    }
}
