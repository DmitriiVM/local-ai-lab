package com.dmitriim.localailab.feature.models.impl.data.transfer

import com.dmitriim.localailab.core.model.library.CatalogArchiveFormat
import com.dmitriim.localailab.core.model.library.CatalogDownloadArchive
import com.dmitriim.localailab.core.model.library.CatalogDownloadFile
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.feature.models.impl.data.library.InstalledModelService
import java.io.File
import kotlinx.coroutines.CancellationException

/** Retrieves, verifies, and atomically installs one catalog model after its transfer is claimed. */
internal class ModelTransferExecution(
    private val installedModels: InstalledModelService,
    private val transferState: ModelTransferStateStore,
    private val fileDownloader: ModelFileDownloader,
    private val ensureRunning: suspend (ModelId, Long) -> Unit,
    private val publishProgress: suspend (StoredModelTransfer) -> Unit,
) {
    suspend fun downloadAndInstall(
        entry: CatalogModel,
        initialTransfer: StoredModelTransfer,
        stagingDirectory: File,
        authorizationToken: String?,
    ) {
        var transfer = initialTransfer
        val modelId = entry.manifest.modelId
        val archive = entry.download.archive
        if (archive != null) {
            transfer = downloadAndExtractArchive(entry, archive, stagingDirectory, transfer, authorizationToken)
        } else {
            val downloads = entry.download.files.ifEmpty {
                val file = entry.manifest.files.single()
                listOf(CatalogDownloadFile(file.relativePath, requireNotNull(entry.download.url)))
            }
            val specifications = entry.manifest.files.associateBy(ModelFileSpec::relativePath)
            require(downloads.sumOf { requireNotNull(specifications[it.relativePath]?.expectedBytes) } == entry.download.expectedBytes)
            var completed = 0L
            val validators = transferState.fileValidators(modelId)
            downloads.forEach { download ->
                ensureRunning(modelId, transfer.executionGeneration)
                val specification = requireNotNull(specifications[download.relativePath]) { "The catalog download is undeclared." }
                val expectedBytes = requireNotNull(specification.expectedBytes) { "The catalog size is missing." }
                val expectedSha256 = requireNotNull(specification.sha256) { "The catalog checksum is missing." }
                val destination = destinationFor(stagingDirectory, download.relativePath)
                val fileValidator = validators[download.relativePath]
                if (fileValidator?.verified == true && destination.length() == expectedBytes) {
                    completed += expectedBytes
                    transfer = transferState.updateWhileRunning(
                        transfer,
                        completedBytes = completed,
                        currentRelativePath = download.relativePath,
                    ) ?: throw CancellationException("The download was paused.")
                    publishProgress(transfer)
                    return@forEach
                }
                transfer = fileDownloader.download(
                    ModelFileDownloadRequest(
                        transfer = transfer,
                        url = download.url,
                        destination = destination,
                        relativePath = download.relativePath,
                        expectedBytes = expectedBytes,
                        expectedSha256 = expectedSha256,
                        completedBeforeFile = completed,
                        authorizationToken = authorizationToken,
                        existingValidator = fileValidator,
                    ),
                )
                completed += expectedBytes
            }
        }
        ensureRunning(modelId, transfer.executionGeneration)
        transfer = transferState.updateWhileRunning(
            transfer,
            status = PersistedModelTransferStatus.INSTALLING,
            message = null,
        ) ?: throw CancellationException("The download was paused before installation.")
        installedModels.installDirectory(
            entry.manifest.copy(installedAtEpochMs = System.currentTimeMillis()),
            stagingDirectory,
            verifyChecksums = archive != null,
        )
        transferState.delete(modelId)
    }

    private suspend fun downloadAndExtractArchive(
        entry: CatalogModel,
        archive: CatalogDownloadArchive,
        stagingDirectory: File,
        transfer: StoredModelTransfer,
        authorizationToken: String?,
    ): StoredModelTransfer {
        require(archive.expectedBytes == entry.download.expectedBytes) { "The catalog archive size does not match." }
        val relativePath = if (archive.format == CatalogArchiveFormat.ZIP) ARCHIVE_ZIP else ARCHIVE_TAR_BZIP2
        val archiveFile = destinationFor(stagingDirectory, relativePath)
        val existing = transferState.fileValidators(entry.manifest.modelId)[relativePath]
        val downloaded = if (existing?.verified == true && archiveFile.length() == archive.expectedBytes) {
            val updated = transferState.updateWhileRunning(
                transfer,
                completedBytes = archive.expectedBytes,
                currentRelativePath = relativePath,
            ) ?: throw CancellationException("The download was paused.")
            publishProgress(updated)
            updated
        } else {
            fileDownloader.download(
                ModelFileDownloadRequest(
                    transfer = transfer,
                    url = archive.url,
                    destination = archiveFile,
                    relativePath = relativePath,
                    expectedBytes = archive.expectedBytes,
                    expectedSha256 = archive.sha256,
                    completedBeforeFile = 0L,
                    authorizationToken = authorizationToken,
                    existingValidator = existing,
                ),
            )
        }
        ensureRunning(entry.manifest.modelId, downloaded.executionGeneration)
        ModelArchiveExtractor.extract(archiveFile, stagingDirectory, archive.rootDirectory, archive.format)
        require(archiveFile.delete()) { "Could not remove the verified model archive." }
        return downloaded
    }

    private fun destinationFor(root: File, relativePath: String): File = File(root, relativePath).also { destination ->
        require(destination.canonicalPath.startsWith(root.canonicalPath + File.separator)) {
            "The model manifest contains an unsafe path."
        }
    }

    private companion object {
        const val ARCHIVE_ZIP = ".download.zip"
        const val ARCHIVE_TAR_BZIP2 = ".download.tar.bz2"
    }
}
