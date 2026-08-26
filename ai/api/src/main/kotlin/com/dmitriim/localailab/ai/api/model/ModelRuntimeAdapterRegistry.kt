package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import dev.zacsweers.metro.Inject

/** Resolves one adapter per persisted engine/profile pair and rejects ambiguous app packaging. */
@Inject
class ModelRuntimeAdapterRegistry(adapters: Set<ModelRuntimeAdapter>) {
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

    fun find(engineId: EngineId, profileType: ModelProfileId): ModelRuntimeAdapter? = byEngineAndProfile[engineId to profileType]
}
