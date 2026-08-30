package com.dmitriim.localailab.feature.models.impl.models.domain.transfer

import com.dmitriim.localailab.ai.api.model.library.CatalogModel

/** The app-private storage required to download and install one catalog model from an empty staging area. */
data class ModelDownloadStorageEstimate(
    val downloadBytes: Long,
    val temporaryExtractionBytes: Long,
    val safetyReserveBytes: Long,
) {
    val peakRequiredBytes: Long = downloadBytes
        .saturatingAdd(temporaryExtractionBytes)
        .saturatingAdd(safetyReserveBytes)
}

/** Estimates peak storage, including archive extraction and the downloader's safety reserve. */
fun CatalogModel.downloadStorageEstimate(): ModelDownloadStorageEstimate {
    val archive = download.archive
    val extractionBytes = archive?.let {
        val declaredFileBytes = manifest.files.sumOf { file -> file.expectedBytes ?: 0L }
        if (manifest.files.any { file -> file.expectedBytes == null }) {
            maxOf(declaredFileBytes, it.expectedBytes)
        } else {
            declaredFileBytes
        }
    } ?: 0L
    return ModelDownloadStorageEstimate(
        downloadBytes = download.expectedBytes,
        temporaryExtractionBytes = extractionBytes,
        safetyReserveBytes = maxOf(MINIMUM_SAFETY_RESERVE_BYTES, download.expectedBytes / SAFETY_RESERVE_DIVISOR),
    )
}

private fun Long.saturatingAdd(other: Long): Long = if (this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other

private const val MINIMUM_SAFETY_RESERVE_BYTES = 256L * 1024 * 1024
private const val SAFETY_RESERVE_DIVISOR = 10L
