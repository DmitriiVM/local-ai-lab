package com.dmitriim.localaiplayground.core.model.capability

import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability
import kotlinx.serialization.Serializable

@Serializable
enum class AiCapability {
    CHAT,
    SPEECH_TO_TEXT,
    TEXT_TO_SPEECH,
    VOICE_ACTIVITY_DETECTION,
    /** A composite feature capability, not a capability declared by an individual model. */
    VOICE_ASSISTANT,
}

@Serializable
enum class CapabilityReadinessState {
    READY,
    MODEL_REQUIRED,
    INSTALLING,
    UNSUPPORTED,
    TEMPORARILY_UNAVAILABLE,
}

data class CapabilityReadiness(
    val capability: AiCapability,
    val state: CapabilityReadinessState,
    val explanation: String,
    val engines: List<EngineAvailability>,
)
