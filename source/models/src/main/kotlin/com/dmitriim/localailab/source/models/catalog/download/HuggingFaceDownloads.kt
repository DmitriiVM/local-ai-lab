package com.dmitriim.localailab.source.models.catalog.download

import com.dmitriim.localailab.core.model.library.CatalogDownloadFile

internal fun huggingFaceFiles(
    repository: String,
    revision: String,
    vararg relativePaths: String,
): List<CatalogDownloadFile> = relativePaths.map { relativePath ->
    CatalogDownloadFile(
        relativePath = relativePath,
        url = "https://huggingface.co/$repository/resolve/$revision/$relativePath",
    )
}
