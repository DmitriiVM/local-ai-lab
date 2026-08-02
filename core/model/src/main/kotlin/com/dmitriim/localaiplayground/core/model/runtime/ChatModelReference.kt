package com.dmitriim.localaiplayground.core.model.runtime

import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId

/** A chat model reference that does not assume app-visible model files. */
sealed interface ChatModelReference {
    val modelId: ModelId
    val displayName: String
    val engineId: EngineId
    val profileType: ModelProfileId
    val defaultContextSize: Int

    data class ArtifactBacked(
        override val modelId: ModelId,
        override val displayName: String,
        override val engineId: EngineId,
        override val profileType: ModelProfileId,
        override val defaultContextSize: Int,
        val artifacts: List<ModelArtifactReference>,
    ) : ChatModelReference {
        init {
            require(artifacts.isNotEmpty()) {
                "An artifact-backed model needs at least one artifact."
            }
        }
    }

    data class SystemManaged(
        override val modelId: ModelId,
        override val displayName: String,
        override val engineId: EngineId,
        override val profileType: ModelProfileId,
        override val defaultContextSize: Int,
    ) : ChatModelReference
}
