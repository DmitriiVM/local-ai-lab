package com.dmitriim.localailab.core.model.manifest

import com.dmitriim.localailab.core.model.engine.EngineId
import kotlinx.serialization.Serializable

/** Stable lookup key for one packaged engine/profile integration. */
@Serializable
data class ModelProfileKey(
    val engineId: EngineId,
    val profileId: ModelProfileId,
)
