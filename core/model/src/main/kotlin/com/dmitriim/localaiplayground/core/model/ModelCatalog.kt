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
    val archive: CatalogDownloadArchive? = null,
)

@Serializable
data class CatalogDownloadArchive(
    val url: String,
    val expectedBytes: Long,
    val sha256: String,
    /** The required archive root, removed while installing to the model directory. */
    val rootDirectory: String,
    val format: CatalogArchiveFormat = CatalogArchiveFormat.TAR_BZIP2,
)

@Serializable
enum class CatalogArchiveFormat {
    TAR_BZIP2,
    ZIP,
}

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
