package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadFile

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
