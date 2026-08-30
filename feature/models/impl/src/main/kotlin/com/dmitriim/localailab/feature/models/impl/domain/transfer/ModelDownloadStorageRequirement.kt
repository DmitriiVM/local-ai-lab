package com.dmitriim.localailab.feature.models.impl.domain.transfer

import com.dmitriim.localailab.ai.api.model.library.CatalogModel

/** Calculates the additional free space required to finish a model download and installation. */
internal object ModelDownloadStorageRequirement {
    fun additionalBytes(
        entry: CatalogModel,
        downloadedArchiveBytes: Long = 0L,
        downloadedFileBytes: Long = 0L,
    ): Long {
        val download = entry.download
        val archive = download.archive
        val remainingDownloadBytes = if (archive != null) {
            (archive.expectedBytes - downloadedArchiveBytes.coerceIn(0L, archive.expectedBytes))
                .coerceAtLeast(0L)
        } else {
            (download.expectedBytes - downloadedFileBytes.coerceAtMost(download.expectedBytes)).coerceAtLeast(0L)
        }
        val estimate = entry.downloadStorageEstimate()
        return remainingDownloadBytes
            .saturatingAdd(estimate.temporaryExtractionBytes)
            .saturatingAdd(estimate.safetyReserveBytes)
    }

    private fun Long.saturatingAdd(other: Long): Long = if (this > Long.MAX_VALUE - other) {
        Long.MAX_VALUE
    } else {
        this + other
    }
}
