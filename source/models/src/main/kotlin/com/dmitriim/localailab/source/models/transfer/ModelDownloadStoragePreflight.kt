package com.dmitriim.localailab.source.models.transfer

import android.os.StatFs
import com.dmitriim.localailab.core.model.library.CatalogArchiveFormat
import com.dmitriim.localailab.core.model.library.CatalogModel
import java.io.File
import java.util.Locale

/** Checks that the app-private volume can hold the remaining transfer and its installation staging. */
internal class ModelDownloadStoragePreflight(
    private val storageDirectory: File,
) {
    fun requireSpaceFor(entry: CatalogModel, stagingDirectory: File) {
        val requiredBytes = requiredAdditionalBytes(entry, stagingDirectory)
        val availableBytes = StatFs(storageDirectory.absolutePath).availableBytes
        require(availableBytes >= requiredBytes) {
            "Not enough app storage to download ${entry.manifest.displayName}. " +
                "Need ${requiredBytes.toReadableStorageBytes()} free, but only " +
                "${availableBytes.toReadableStorageBytes()} is available."
        }
    }

    private fun requiredAdditionalBytes(entry: CatalogModel, stagingDirectory: File): Long {
        val download = entry.download
        val remainingDownloadBytes = download.archive?.let { archive ->
            val archiveFile = File(
                stagingDirectory,
                if (archive.format == CatalogArchiveFormat.ZIP) ARCHIVE_ZIP else ARCHIVE_TAR_BZIP2,
            )
            (archive.expectedBytes - archiveFile.length().coerceIn(0L, archive.expectedBytes)).coerceAtLeast(0L)
        } ?: (download.expectedBytes - stagingDirectory.totalFileBytes().coerceAtMost(download.expectedBytes))
            .coerceAtLeast(0L)

        val extractionBytes = download.archive?.let { archive ->
            val declaredFileBytes = entry.manifest.files.sumOf { it.expectedBytes ?: 0L }
            if (entry.manifest.files.any { it.expectedBytes == null }) {
                // Archives with directory-only manifest entries cannot state their complete expanded size.
                // Reserving at least the archive size keeps that catalog shape conservative.
                maxOf(declaredFileBytes, archive.expectedBytes)
            } else {
                declaredFileBytes
            }
        } ?: 0L

        return remainingDownloadBytes
            .saturatingAdd(extractionBytes)
            .saturatingAdd(safetyReserveBytes(entry.download.expectedBytes))
    }

    private fun safetyReserveBytes(downloadBytes: Long): Long = maxOf(
        MINIMUM_SAFETY_RESERVE_BYTES,
        downloadBytes / SAFETY_RESERVE_DIVISOR,
    )

    private fun File.totalFileBytes(): Long = walkTopDown()
        .filter(File::isFile)
        .fold(0L) { total, file -> total.saturatingAdd(file.length()) }

    private fun Long.saturatingAdd(other: Long): Long =
        if (this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other

    private fun Long.toReadableStorageBytes(): String = when {
        this >= GIBIBYTE -> String.format(Locale.US, "%.1f GiB", toDouble() / GIBIBYTE)
        this >= MEBIBYTE -> String.format(Locale.US, "%.1f MiB", toDouble() / MEBIBYTE)
        else -> "$this B"
    }

    private companion object {
        const val ARCHIVE_ZIP = ".download.zip"
        const val ARCHIVE_TAR_BZIP2 = ".download.tar.bz2"
        const val MINIMUM_SAFETY_RESERVE_BYTES = 256L * 1024 * 1024
        const val SAFETY_RESERVE_DIVISOR = 10L
        const val MEBIBYTE = 1024L * 1024
        const val GIBIBYTE = 1024L * MEBIBYTE
    }
}
