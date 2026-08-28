package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.core.model.library.CatalogModel

/**
 * One app-bundled catalog model and the runtime profile required to install and execute it.
 *
 * A contribution is the complete registration point for a downloadable model. [runtimeProfile]
 * must describe the same engine/profile key and capabilities declared by [catalogModel].
 */
interface ModelCatalogContribution {
    val catalogModel: CatalogModel
    val runtimeProfile: ModelRuntimeProfile
}
