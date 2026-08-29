package com.dmitriim.localailab.ai.api.model.manifest

import com.dmitriim.localailab.ai.api.engine.EngineId
import kotlinx.serialization.Serializable

/** Stable lookup key for one packaged engine/profile integration. */
@Serializable
data class ModelProfileKey(
    val engineId: EngineId,
    val profileId: ModelProfileId,
)
