package com.dmitriim.localaiplayground.core.model.engine

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import kotlinx.serialization.Serializable

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
