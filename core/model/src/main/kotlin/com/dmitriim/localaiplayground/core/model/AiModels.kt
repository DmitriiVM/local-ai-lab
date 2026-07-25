package com.dmitriim.localaiplayground.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AiCapability {
    CHAT,
    SPEECH_TO_TEXT,
    TEXT_TO_SPEECH,
    VOICE_ASSISTANT,
}

@Serializable
@JvmInline
value class EngineId(val value: String)

@Serializable
enum class EngineKind {
    CUSTOM,
    SYSTEM,
}

@Serializable
enum class RuntimeBackend {
    CPU,
    VULKAN,
    NNAPI,
    SYSTEM_SERVICE,
}

@Serializable
data class EngineDescriptor(
    val id: EngineId,
    val displayName: String,
    val kind: EngineKind,
    val capabilities: Set<AiCapability>,
    val bundledRuntime: Boolean,
)

sealed interface EngineAvailability {
    val descriptor: EngineDescriptor

    data class Available(
        override val descriptor: EngineDescriptor,
        val effectiveBackend: RuntimeBackend,
        val detail: String,
        val requestedBackend: RuntimeBackend = effectiveBackend,
        val effectiveThreadCount: Int? = null,
        val fallbackReason: String? = null,
    ) : EngineAvailability

    data class Unsupported(
        override val descriptor: EngineDescriptor,
        val reason: String,
    ) : EngineAvailability

    data class TemporarilyUnavailable(
        override val descriptor: EngineDescriptor,
        val reason: String,
    ) : EngineAvailability
}

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

@Serializable
@JvmInline
value class ModelId(val value: String)

@Serializable
enum class ModelLifecycleState {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    LOADED,
    INVALID,
}
