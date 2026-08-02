package com.dmitriim.localaiplayground.ai.api.model

import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import dev.zacsweers.metro.Inject

/** Resolves one adapter per persisted engine/profile pair and rejects ambiguous app packaging. */
@Inject
class ModelAdapterRegistry(adapters: Set<ModelAdapter>) {
    init {
        require(adapters.map { it.id }.distinct().size == adapters.size) {
            "More than one packaged model adapter declares the same adapter ID."
        }
    }

    private val byEngineAndProfile = buildMap {
        adapters.forEach { adapter ->
            adapter.profileTypes.forEach { profileType ->
                val key = adapter.engineId to profileType
                require(put(key, adapter) == null) {
                    "More than one packaged model adapter declares ${adapter.engineId.value}/${profileType.value}."
                }
            }
        }
    }

    fun find(engineId: EngineId, profileType: ModelProfileId): ModelAdapter? =
        byEngineAndProfile[engineId to profileType]
}
