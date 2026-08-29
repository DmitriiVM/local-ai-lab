package com.dmitriim.localailab.feature.models.impl.data.transfer

import android.os.StatFs
import com.dmitriim.localailab.ai.api.model.library.CatalogArchiveFormat
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.feature.models.impl.domain.transfer.ModelDownloadStorageRequirement
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
        val downloadedArchiveBytes = entry.download.archive?.let { archive ->
            val archiveFile = File(
                stagingDirectory,
                if (archive.format == CatalogArchiveFormat.ZIP) ARCHIVE_ZIP else ARCHIVE_TAR_BZIP2,
            )
            archiveFile.length()
        } ?: 0L
        return ModelDownloadStorageRequirement.additionalBytes(
            entry = entry,
            downloadedArchiveBytes = downloadedArchiveBytes,
            downloadedFileBytes = stagingDirectory.totalFileBytes(),
        )
    }

    private fun File.totalFileBytes(): Long = walkTopDown()
        .filter(File::isFile)
        .fold(0L) { total, file -> total.saturatingAdd(file.length()) }

    private fun Long.saturatingAdd(other: Long): Long = if (this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other

    private fun Long.toReadableStorageBytes(): String = when {
        this >= GIBIBYTE -> String.format(Locale.US, "%.1f GiB", toDouble() / GIBIBYTE)
        this >= MEBIBYTE -> String.format(Locale.US, "%.1f MiB", toDouble() / MEBIBYTE)
        else -> "$this B"
    }

    private companion object {
        const val ARCHIVE_ZIP = ".download.zip"
        const val ARCHIVE_TAR_BZIP2 = ".download.tar.bz2"
        const val MEBIBYTE = 1024L * 1024
        const val GIBIBYTE = 1024L * MEBIBYTE
    }
}
