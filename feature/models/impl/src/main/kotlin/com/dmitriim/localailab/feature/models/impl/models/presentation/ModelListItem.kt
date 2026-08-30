package com.dmitriim.localailab.feature.models.impl.models.presentation

import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel

internal sealed interface ModelListItem {
    val manifest: ModelManifest

    data class Installed(
        val model: InstalledModel,
        val catalogModel: CatalogModel? = null,
    ) : ModelListItem {
        override val manifest: ModelManifest = catalogModel?.manifest ?: model.manifest
    }

    data class Catalog(val model: CatalogModel) : ModelListItem {
        override val manifest: ModelManifest = model.manifest
    }
}

internal fun ModelsUiState.toModelListItems(): List<ModelListItem> {
    val installedById = installed.associateBy { it.manifest.modelId }
    val catalogModelIds = catalog.mapTo(mutableSetOf()) { it.manifest.modelId }
    return buildList {
        catalog.forEach { catalogModel ->
            val installedModel = installedById[catalogModel.manifest.modelId]
            add(
                installedModel?.let { ModelListItem.Installed(it, catalogModel) }
                    ?: ModelListItem.Catalog(catalogModel),
            )
        }
        installed
            .filterNot { it.manifest.modelId in catalogModelIds }
            .forEach { add(ModelListItem.Installed(it)) }
    }.sortedWith(
        compareBy<ModelListItem> { it.manifest.displayName.lowercase() }
            .thenBy { it.manifest.modelId.value },
    )
}
