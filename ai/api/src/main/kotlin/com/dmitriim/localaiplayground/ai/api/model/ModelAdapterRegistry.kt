package com.dmitriim.localaiplayground.ai.api.model

import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import dev.zacsweers.metro.Inject

/** Resolves one adapter per persisted profile ID and rejects ambiguous app packaging. */
@Inject
class ModelAdapterRegistry(adapters: Set<ModelAdapter>) {
    init {
        require(adapters.map { it.id }.distinct().size == adapters.size) {
            "More than one packaged model adapter declares the same adapter ID."
        }
    }

    private val byProfile = buildMap {
        adapters.forEach { adapter ->
            adapter.profileTypes.forEach { profileType ->
                require(put(profileType, adapter) == null) {
                    "More than one packaged model adapter declares ${profileType.value}."
                }
            }
        }
    }

    fun find(profileType: ModelProfileId): ModelAdapter? = byProfile[profileType]
}
