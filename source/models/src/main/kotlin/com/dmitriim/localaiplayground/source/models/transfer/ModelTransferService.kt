package com.dmitriim.localaiplayground.source.models.transfer

import android.app.Application
import android.util.Log
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.di.ApplicationCoroutineScope
import com.dmitriim.localaiplayground.core.model.library.CatalogArchiveFormat
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadArchive
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadAuthentication
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadFile
import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.ModelCompatibilityState
import com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy
import com.dmitriim.localaiplayground.core.model.library.ModelTransferState
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.service.ModelTransfers
import com.dmitriim.localaiplayground.source.models.catalog.ModelCatalog
import com.dmitriim.localaiplayground.source.models.credentials.HuggingFaceTokenStore
import com.dmitriim.localaiplayground.source.models.diagnostics.ModelDiagnosticsService
import com.dmitriim.localaiplayground.source.models.library.InstalledModelService
import com.dmitriim.localaiplayground.source.models.library.ModelImportPolicy
import com.dmitriim.localaiplayground.source.models.validation.sha256
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns persisted model download state, resumable transfer, and transactional installation. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ModelTransfers>())
class ModelTransferService(
    private val application: Application,
    private val installedModels: InstalledModelService,
    private val diagnostics: ModelDiagnosticsService,
    private val transferState: ModelTransferStateStore,
    private val huggingFaceTokens: HuggingFaceTokenStore,
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : ModelTransfers,
    ModelDownloadExecutor {
    override val catalog: Flow<List<CatalogModel>> = MutableStateFlow(ModelCatalog.entries).asStateFlow()
    override val transfers: Flow<Map<ModelId, ModelTransferState>> = transferState.transfers

    init {
        ModelDownloadRuntime.executor = this
        applicationScope.launch(Dispatchers.IO) {
            ModelTransferRecovery(application, installedModels, transferState).reconcile()
        }
    }

    override suspend fun download(
        modelId: ModelId,
        networkPolicy: ModelTransferNetworkPolicy,
    ): Result<Unit> = runCatching {
        val entry = catalogEntry(modelId)
        if (installedModels.registerInstalledDirectory(modelId)) {
            transferState.delete(modelId)
            return@runCatching
        }
        val compatibility = diagnostics.compatibility(entry.manifest)
        require(compatibility.state != ModelCompatibilityState.INCOMPATIBLE) { compatibility.reasons.joinToString() }
        val existing = transferState.find(modelId)
        when (existing?.status) {
            PersistedModelTransferStatus.QUEUED,
            PersistedModelTransferStatus.RUNNING,
            PersistedModelTransferStatus.INSTALLING,
            -> return@runCatching
            PersistedModelTransferStatus.FAILED -> {
                stagingDirectory(modelId).deleteRecursively()
                transferState.delete(modelId)
            }
            null,
            PersistedModelTransferStatus.PAUSED,
            -> Unit
        }
        queue(entry, networkPolicy)
    }

    override suspend fun pauseTransfer(modelId: ModelId) {
        val transfer = transferState.find(modelId) ?: return
        if (transfer.status == PersistedModelTransferStatus.INSTALLING) return
        transferState.update(
            transfer,
            status = PersistedModelTransferStatus.PAUSED,
            message = "Paused by user.",
        )
        ModelTransferScheduler(application).cancel(modelId)
    }

    override suspend fun resumeTransfer(
        modelId: ModelId,
        networkPolicy: ModelTransferNetworkPolicy,
    ): Result<Unit> = runCatching {
        val entry = catalogEntry(modelId)
        val transfer = requireNotNull(transferState.find(modelId)) { "This download is no longer available." }
        require(transfer.status != PersistedModelTransferStatus.INSTALLING) { "The model is being installed." }
        require(transfer.catalogVersion == requireNotNull(entry.manifest.catalogVersion) && transfer.revision == entry.manifest.revision) {
            "The bundled catalog changed. Restart this download."
        }
        queue(entry, networkPolicy)
    }

    override suspend fun cancelTransfer(modelId: ModelId) {
        val transfer = transferState.find(modelId) ?: return
        if (transfer.status == PersistedModelTransferStatus.INSTALLING) return
        transferState.delete(modelId)
        ModelTransferScheduler(application).cancel(modelId)
        stagingDirectory(modelId).deleteRecursively()
    }

    override suspend fun executeScheduledDownload(modelId: ModelId, executionGeneration: Long) {
        val transfer = transferState.find(modelId) ?: return
        if (!transferState.claimQueued(modelId, executionGeneration)) return
        val claimed = requireNotNull(transferState.find(modelId))
        try {
            val entry = catalogEntry(modelId)
            require(claimed.catalogVersion == requireNotNull(entry.manifest.catalogVersion) && claimed.revision == entry.manifest.revision) {
                "The bundled catalog changed. Restart this download."
            }
            withContext(Dispatchers.IO) { downloadAndInstall(entry, claimed) }
        } catch (cancelled: CancellationException) {
            val latest = transferState.find(modelId)
            if (latest?.status == PersistedModelTransferStatus.RUNNING && latest.executionGeneration == executionGeneration) {
                transferState.update(
                    latest,
                    status = PersistedModelTransferStatus.PAUSED,
                    message = "Download interrupted. Tap Resume to continue.",
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            val latest = transferState.find(modelId) ?: return
            val retryable = error.isRetryableDownloadFailure()
            transferState.update(
                latest,
                status = if (retryable) PersistedModelTransferStatus.PAUSED else PersistedModelTransferStatus.FAILED,
                message = error.message ?: if (retryable) "Download interrupted. Tap Resume to continue." else "Download failed.",
            )
            Log.e(TAG, "Catalog model transfer failed: modelId=${modelId.value}", error)
        }
    }

    private suspend fun queue(entry: CatalogModel, networkPolicy: ModelTransferNetworkPolicy) {
        val modelId = entry.manifest.modelId
        require(stagingDirectory(modelId).mkdirs() || stagingDirectory(modelId).isDirectory) {
            "Could not prepare the download directory."
        }
        val queued = transferState.queueNew(
            modelId = modelId,
            catalogVersion = requireNotNull(entry.manifest.catalogVersion),
            revision = entry.manifest.revision,
            totalBytes = entry.download.expectedBytes,
            networkPolicy = networkPolicy,
        )
        try {
            ModelTransferScheduler(application).schedule(entry, queued.executionGeneration, networkPolicy)
        } catch (error: Throwable) {
            transferState.update(
                queued,
                status = PersistedModelTransferStatus.FAILED,
                message = error.message ?: "Download could not be scheduled.",
            )
            throw error
        }
    }

    private suspend fun downloadAndInstall(entry: CatalogModel, initialTransfer: StoredModelTransfer) {
        var transfer = initialTransfer
        val modelId = entry.manifest.modelId
        val temporary = stagingDirectory(modelId)
        val authorizationToken = credentialFor(entry)
        val archive = entry.download.archive
        if (archive != null) {
            transfer = downloadAndExtractArchive(entry, archive, temporary, transfer, authorizationToken)
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
                val destination = destinationFor(temporary, download.relativePath)
                val fileValidator = validators[download.relativePath]
                if (fileValidator?.verified == true && destination.length() == expectedBytes) {
                    completed += expectedBytes
                    transfer = transferState.updateWhileRunning(
                        transfer,
                        completedBytes = completed,
                        currentRelativePath = download.relativePath,
                    ) ?: throw CancellationException("The download was paused.")
                    return@forEach
                }
                transfer = downloadFileWithRetries(
                    transfer = transfer,
                    url = download.url,
                    destination = destination,
                    relativePath = download.relativePath,
                    expectedBytes = expectedBytes,
                    expectedSha256 = expectedSha256,
                    completedBeforeFile = completed,
                    authorizationToken = authorizationToken,
                    existingValidator = fileValidator,
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
            temporary,
            verifyChecksums = archive != null,
        )
        transferState.delete(modelId)
    }

    private suspend fun downloadAndExtractArchive(
        entry: CatalogModel,
        archive: CatalogDownloadArchive,
        temporary: File,
        transfer: StoredModelTransfer,
        authorizationToken: String?,
    ): StoredModelTransfer {
        require(archive.expectedBytes == entry.download.expectedBytes) { "The catalog archive size does not match." }
        val relativePath = if (archive.format == CatalogArchiveFormat.ZIP) ARCHIVE_ZIP else ARCHIVE_TAR_BZIP2
        val archiveFile = destinationFor(temporary, relativePath)
        val existing = transferState.fileValidators(entry.manifest.modelId)[relativePath]
        val downloaded = if (existing?.verified == true && archiveFile.length() == archive.expectedBytes) {
            transferState.updateWhileRunning(
                transfer,
                completedBytes = archive.expectedBytes,
                currentRelativePath = relativePath,
            ) ?: throw CancellationException("The download was paused.")
        } else {
            downloadFileWithRetries(
                transfer = transfer,
                url = archive.url,
                destination = archiveFile,
                relativePath = relativePath,
                expectedBytes = archive.expectedBytes,
                expectedSha256 = archive.sha256,
                completedBeforeFile = 0L,
                authorizationToken = authorizationToken,
                existingValidator = existing,
            )
        }
        ensureRunning(entry.manifest.modelId, downloaded.executionGeneration)
        ModelArchiveExtractor.extract(archiveFile, temporary, archive.rootDirectory, archive.format)
        require(archiveFile.delete()) { "Could not remove the verified model archive." }
        return downloaded
    }

    private suspend fun downloadFileWithRetries(
        transfer: StoredModelTransfer,
        url: String,
        destination: File,
        relativePath: String,
        expectedBytes: Long,
        expectedSha256: String,
        completedBeforeFile: Long,
        authorizationToken: String?,
        existingValidator: com.dmitriim.localaiplayground.source.database.ModelTransferFileEntity?,
    ): StoredModelTransfer {
        var current = transfer
        var failure: Throwable? = null
        for (attempt in RETRY_DELAYS_MS.indices) {
            if (attempt > 0) {
                val retryDelay = maxOf(RETRY_DELAYS_MS[attempt], (failure as? ModelDownloadFailure)?.retryAfterMillis ?: 0L)
                delay(retryDelay)
            }
            try {
                current = downloadFile(
                    transfer = current,
                    url = url,
                    destination = destination,
                    relativePath = relativePath,
                    expectedBytes = expectedBytes,
                    completedBeforeFile = completedBeforeFile,
                    authorizationToken = authorizationToken,
                    existingValidator = existingValidator,
                )
                require(destination.length() == expectedBytes) { "Downloaded size for $relativePath does not match the catalog." }
                require(destination.sha256().equals(expectedSha256, ignoreCase = true)) {
                    "Downloaded checksum for $relativePath does not match the catalog."
                }
                transferState.markFileVerified(current.modelIdAsModelId(), relativePath)
                return current
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                failure = error
                if (!error.isRetryableDownloadFailure() || attempt == RETRY_DELAYS_MS.lastIndex) break
            }
        }
        throw requireNotNull(failure)
    }

    private suspend fun downloadFile(
        transfer: StoredModelTransfer,
        url: String,
        destination: File,
        relativePath: String,
        expectedBytes: Long,
        completedBeforeFile: Long,
        authorizationToken: String?,
        existingValidator: com.dmitriim.localaiplayground.source.database.ModelTransferFileEntity?,
    ): StoredModelTransfer {
        destination.parentFile?.let { require(it.isDirectory || it.mkdirs()) }
        var offset = destination.length().coerceAtMost(expectedBytes)
        if (destination.length() > expectedBytes) destination.delete()
        offset = destination.length()
        var resetUsed = false
        while (true) {
            ensureRunning(transfer.modelIdAsModelId(), transfer.executionGeneration)
            val validator = transferState.fileValidators(transfer.modelIdAsModelId())[relativePath] ?: existingValidator
            val response = openResponse(url, offset, validator, authorizationToken, transfer.modelIdAsModelId())
            response.connection.useResponse { connection ->
                val status = connection.responseCode
                if (status == HTTP_RANGE_NOT_SATISFIABLE) {
                    if (offset == expectedBytes) return transfer
                    if (resetUsed) failDownload(ModelDownloadFailure("Server rejected the download range.", retryable = false))
                    destination.delete()
                    offset = 0L
                    resetUsed = true
                    return@useResponse
                }
                if (status !in 200..299) failDownload(httpFailure(status, connection))
                val append = when (status) {
                    HttpURLConnection.HTTP_OK -> {
                        if (offset > 0) {
                            if (resetUsed) {
                                failDownload(
                                    ModelDownloadFailure(
                                        "Server does not support resuming this file.",
                                        retryable = false,
                                    ),
                                )
                            }
                            destination.delete()
                            offset = 0L
                            resetUsed = true
                        }
                        false
                    }
                    HttpURLConnection.HTTP_PARTIAL -> {
                        validateContentRange(connection.getHeaderField("Content-Range"), offset, expectedBytes)
                        true
                    }
                    else -> failDownload(ModelDownloadFailure("Unexpected HTTP $status response.", retryable = false))
                }
                val announced = connection.getHeaderFieldLong("Content-Length", -1)
                val expectedResponseBytes = if (append) expectedBytes - offset else expectedBytes
                if (announced >= 0 && announced != expectedResponseBytes) {
                    failDownload(ModelDownloadFailure("Server response length does not match the catalog.", retryable = false))
                }
                val eTag = connection.getHeaderField("ETag")
                val lastModified = connection.getHeaderField("Last-Modified")
                if (append && validator?.eTag != null && eTag != null && validator.eTag != eTag) {
                    if (resetUsed) {
                        failDownload(
                            ModelDownloadFailure(
                                "Server changed the download while it was paused.",
                                retryable = false,
                            ),
                        )
                    }
                    destination.delete()
                    offset = 0L
                    resetUsed = true
                    return@useResponse
                }
                transferState.updateFileValidator(transfer.modelIdAsModelId(), relativePath, eTag, lastModified, verified = false)
                var current = transfer
                var written = offset
                var lastReportedBytes = written
                var lastReportedAt = System.currentTimeMillis()
                connection.inputStream.use { input ->
                    FileOutputStream(destination, append).buffered(FILE_BUFFER_BYTES).use { output ->
                        val buffer = ByteArray(FILE_BUFFER_BYTES)
                        while (true) {
                            ensureRunning(transfer.modelIdAsModelId(), transfer.executionGeneration)
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            val now = System.currentTimeMillis()
                            if (written - lastReportedBytes >= PROGRESS_BYTES || now - lastReportedAt >= PROGRESS_INTERVAL_MS) {
                                current = transferState.updateWhileRunning(
                                    current,
                                    completedBytes = completedBeforeFile + written,
                                    currentRelativePath = relativePath,
                                    message = null,
                                ) ?: failDownload(CancellationException("The download was paused."))
                                lastReportedBytes = written
                                lastReportedAt = now
                            }
                        }
                    }
                }
                if (written != expectedBytes) {
                    failDownload(ModelDownloadFailure("Download ended before the expected size.", retryable = true))
                }
                return transferState.updateWhileRunning(
                    current,
                    completedBytes = completedBeforeFile + written,
                    currentRelativePath = relativePath,
                    message = null,
                ) ?: failDownload(CancellationException("The download was paused."))
            }
        }
    }

    private suspend fun ensureRunning(modelId: ModelId, generation: Long) {
        coroutineContext.ensureActive()
        val transfer = transferState.find(modelId)
        if (transfer?.status != PersistedModelTransferStatus.RUNNING || transfer.executionGeneration != generation) {
            throw CancellationException("The download is no longer active.")
        }
    }

    private fun openResponse(
        url: String,
        offset: Long,
        validator: com.dmitriim.localaiplayground.source.database.ModelTransferFileEntity?,
        authorizationToken: String?,
        modelId: ModelId,
    ): DownloadResponse {
        var currentUrl = url
        repeat(MAX_REDIRECTS) { redirect ->
            val uri = URI(currentUrl)
            require(uri.scheme == HTTPS_SCHEME) { "Model downloads must use HTTPS." }
            val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept-Encoding", "identity")
                if (offset > 0) {
                    setRequestProperty("Range", "bytes=$offset-")
                    validator?.eTag?.takeIf { it.startsWith('"') && !it.startsWith("W/", ignoreCase = true) }
                        ?.let { setRequestProperty("If-Range", it) }
                        ?: validator?.lastModified?.let { setRequestProperty("If-Range", it) }
                }
                if (uri.host == HUGGING_FACE_HOST && authorizationToken != null) {
                    setRequestProperty("Authorization", "Bearer $authorizationToken")
                }
            }
            val status = connection.responseCode
            if (status !in 300..399) return DownloadResponse(connection)
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            val redirectUri = requireNotNull(location) { "Redirect without a target." }.let(uri::resolve)
            require(redirectUri.scheme == HTTPS_SCHEME) { "Model downloads must use HTTPS." }
            currentUrl = redirectUri.toString()
            Log.i(TAG, "Catalog model redirected: modelId=${modelId.value}, redirect=${redirect + 1}")
        }
        throw ModelDownloadFailure("The download redirected too many times.", retryable = false)
    }

    private fun stagingRoot(): File = File(application.filesDir, STAGING_DIRECTORY_NAME)

    private fun stagingDirectory(modelId: ModelId): File = File(stagingRoot(), ModelImportPolicy.directoryName(modelId))

    private fun destinationFor(root: File, relativePath: String): File = File(root, relativePath).also { destination ->
        require(destination.canonicalPath.startsWith(root.canonicalPath + File.separator)) { "The model manifest contains an unsafe path." }
    }

    private fun credentialFor(entry: CatalogModel): String? = when (entry.download.authentication) {
        CatalogDownloadAuthentication.NONE -> null
        CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN -> huggingFaceTokens.tokenOrNull()
            ?: throw ModelDownloadFailure("A Hugging Face access token is required for this model.", retryable = false)
    }

    private fun catalogEntry(modelId: ModelId) = ModelCatalog.entries.firstOrNull { it.manifest.modelId == modelId }
        ?: error("This catalog model is no longer available in the bundled catalog.")

    private fun StoredModelTransfer.modelIdAsModelId() = ModelId(modelId)

    private fun Throwable.isRetryableDownloadFailure(): Boolean = when (this) {
        is ModelDownloadFailure -> retryable
        is java.io.IOException -> true
        else -> false
    }

    private fun httpFailure(status: Int, connection: HttpURLConnection): ModelDownloadFailure = when (status) {
        HTTP_UNAUTHORIZED -> ModelDownloadFailure("Hugging Face token is invalid or expired.", retryable = false)
        HTTP_FORBIDDEN -> ModelDownloadFailure("Access denied. Accept the model license and verify repository access.", retryable = false)
        else -> ModelDownloadFailure(
            message = "Download failed with HTTP $status.",
            retryable = status == 408 || status == 429 || status >= 500,
            retryAfterMillis = connection.getHeaderField("Retry-After")
                ?.toLongOrNull()
                ?.times(1_000L)
                ?.coerceAtMost(MAX_RETRY_AFTER_MS),
        )
    }

    private fun failDownload(error: Throwable): Nothing = throw error

    private fun validateContentRange(value: String?, offset: Long, expectedBytes: Long) {
        val match = CONTENT_RANGE.matchEntire(requireNotNull(value) { "Missing Content-Range for resumed download." })
            ?: throw ModelDownloadFailure("Invalid Content-Range for resumed download.", retryable = false)
        require(match.groupValues[1].toLong() == offset && match.groupValues[3].toLong() == expectedBytes) {
            "Server resumed a different byte range."
        }
    }

    private class DownloadResponse(val connection: HttpURLConnection)

    private inline fun <T> HttpURLConnection.useResponse(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }

    private companion object {
        const val TAG = "AiP123Models"
        const val STAGING_DIRECTORY_NAME = "model-downloads"
        const val ARCHIVE_ZIP = ".download.zip"
        const val ARCHIVE_TAR_BZIP2 = ".download.tar.bz2"
        const val HUGGING_FACE_HOST = "huggingface.co"
        const val HTTPS_SCHEME = "https"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
        const val MAX_REDIRECTS = 5
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val FILE_BUFFER_BYTES = 256 * 1024
        const val PROGRESS_BYTES = 1L * 1024 * 1024
        const val PROGRESS_INTERVAL_MS = 500L
        const val MAX_RETRY_AFTER_MS = 60_000L
        val RETRY_DELAYS_MS = longArrayOf(0L, 2_000L, 5_000L, 15_000L)
        val CONTENT_RANGE = Regex("bytes (\\d+)-(\\d+)/(\\d+)")
    }
}

internal interface ModelDownloadExecutor {
    suspend fun executeScheduledDownload(modelId: ModelId, executionGeneration: Long)
}
