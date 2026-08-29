package com.dmitriim.localailab.feature.models.impl.data.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.library.CatalogModel
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Immutable app catalog assembled from engine-owned DI contributions. */
@Inject
@SingleIn(AppScope::class)
class ModelCatalogRegistry(
    contributions: Set<ModelCatalogContribution>,
) {
    private val contributionsById = contributions.associateBy { it.catalogModel.manifest.modelId }.also { indexed ->
        require(indexed.size == contributions.size) {
            "More than one catalog contribution declares the same model ID."
        }
    }

    val entries: List<CatalogModel> = contributionsById.values.map(ModelCatalogContribution::catalogModel)

    private val byId = entries.associateBy { it.manifest.modelId }.also { indexed ->
        entries.forEach { entry ->
            val manifest = entry.manifest
            val contribution = checkNotNull(contributionsById[manifest.modelId])
            val profile = contribution.runtimeProfile
            require(
                manifest.engineId == profile.key.engineId &&
                    manifest.profileType == profile.key.profileId,
            ) {
                "Catalog model ${manifest.modelId.value} does not match its runtime profile " +
                    "${profile.key.engineId.value}/${profile.key.profileId.value}."
            }
        }
    }

    fun find(modelId: com.dmitriim.localailab.core.model.manifest.ModelId): CatalogModel? = byId[modelId]
}
