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
    val url: String? = null,
    val expectedBytes: Long,
    val sha256: String? = null,
    val files: List<CatalogDownloadFile> = emptyList(),
)

@Serializable
data class CatalogDownloadFile(
    val relativePath: String,
    val url: String,
)

@Serializable
data class CatalogModel(
    val manifest: ModelManifest,
    val state: ModelCatalogState,
    val download: CatalogDownload,
)
