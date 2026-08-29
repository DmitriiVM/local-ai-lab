package com.dmitriim.localailab.ai.api.engine

import com.dmitriim.localailab.ai.api.capability.AiCapability
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
        val effectiveComputePreference: ComputePreference,
        val detail: String,
        val requestedComputePreference: ComputePreference = effectiveComputePreference,
        /** Runtime-reported compute implementation, such as a delegate or system provider. */
        val computeDetail: String? = null,
        val effectiveThreadCount: Int? = null,
        val fallbackReason: String? = null,
    ) : EngineAvailability

    data class Unsupported(override val descriptor: EngineDescriptor, val reason: String) : EngineAvailability

    data class TemporarilyUnavailable(
        override val descriptor: EngineDescriptor,
        val reason: String,
    ) : EngineAvailability
}
