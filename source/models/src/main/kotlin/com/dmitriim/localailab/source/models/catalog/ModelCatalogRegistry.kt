package com.dmitriim.localailab.source.models.catalog

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelCatalogContribution
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Immutable app catalog assembled from engine-owned DI contributions. */
@Inject
@SingleIn(AppScope::class)
class ModelCatalogRegistry(
    contributions: Set<ModelCatalogContribution>,
    profiles: ModelRuntimeProfileRegistry,
) {
    val entries: List<CatalogModel> = contributions.map(ModelCatalogContribution::catalogModel)

    private val byId = entries.associateBy { it.manifest.modelId }.also { indexed ->
        require(indexed.size == entries.size) {
            "More than one catalog contribution declares the same model ID."
        }
        entries.forEach { entry ->
            val manifest = entry.manifest
            val key = ModelProfileKey(manifest.engineId, manifest.profileType)
            val profile = profiles.requireRuntimeProfile(key)
            require(manifest.capabilities == profile.capabilities) {
                "Catalog model ${manifest.modelId.value} capabilities do not match ${manifest.engineId.value}/${manifest.profileType.value}."
            }
        }
    }

    fun find(modelId: com.dmitriim.localailab.core.model.manifest.ModelId): CatalogModel? = byId[modelId]
}
