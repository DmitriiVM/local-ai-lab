package com.dmitriim.localaiplayground.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModelCatalogState {
    PROVISIONAL,
    APPROVED,
    OPTIONAL,
    IMPORT_ONLY,
    RETIRED,
}

@Serializable
data class CatalogDownload(
    val url: String,
    val expectedBytes: Long,
    val sha256: String,
    val archive: Boolean = false,
)

@Serializable
data class CatalogModel(
    val manifest: ModelManifest,
    val state: ModelCatalogState,
    val download: CatalogDownload,
)
