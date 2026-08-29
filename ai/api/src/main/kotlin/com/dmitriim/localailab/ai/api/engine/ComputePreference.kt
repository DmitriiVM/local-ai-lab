package com.dmitriim.localailab.ai.api.engine

import kotlinx.serialization.Serializable

/** Engine-neutral preference for where inference work should execute. */
@Serializable
enum class ComputePreference {
    AUTO,
    CPU,
    GPU,
    NPU,
    SYSTEM_SERVICE,
}
