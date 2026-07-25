package com.dmitriim.localaiplayground.source.models.transfer

import android.app.Application
import android.util.Log
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.CatalogDownloadFile
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.ModelCompatibilityState
import com.dmitriim.localaiplayground.core.model.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelTransferState
import com.dmitriim.localaiplayground.core.model.ModelTransfers
import com.dmitriim.localaiplayground.source.models.catalog.ModelCatalog
import com.dmitriim.localaiplayground.source.models.diagnostics.ModelDiagnosticsService
import com.dmitriim.localaiplayground.source.models.library.InstalledModelService
import com.dmitriim.localaiplayground.source.models.validation.sha256
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Owns scheduling, network transfer progress, and transactional catalog installation. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ModelTransfers>())
class ModelTransferService(
    private val application: Application,
    private val installedModels: InstalledModelService,
    private val diagnostics: ModelDiagnosticsService,
    private val transferState: ModelTransferStateStore,
) : ModelTransfers, ModelDownloadExecutor {
    override val catalog: Flow<List<CatalogModel>> = MutableStateFlow(ModelCatalog.entries).asStateFlow()
    override val transfers: Flow<Map<ModelId, ModelTransferState>> = transferState.transfers

    init {
        ModelDownloadRuntime.executor = this
    }

    override suspend fun download(modelId: ModelId): Result<Unit> = runCatching {
        val entry = catalogEntry(modelId)
        Log.i(TAG, "Catalog model download requested: modelId=${modelId.value}, files=${entry.download.files.size}, expectedBytes=${entry.download.expectedBytes}")
        if (installedModels.registerInstalledDirectory(modelId)) {
            Log.i(TAG, "Catalog model already installed; marking transfer completed: modelId=${modelId.value}")
            transferState.update { it + (modelId to ModelTransferState.Completed) }
            return@runCatching
        }
        when (transferState.stateFor(modelId)) {
            ModelTransferState.Queued,
            is ModelTransferState.Running,
            ModelTransferState.Installing,
            -> {
                Log.i(TAG, "Catalog model transfer already active: modelId=${modelId.value}")
                return@runCatching
            }
            else -> Unit
        }
        val compatibility = diagnostics.compatibility(entry.manifest)
        Log.i(TAG, "Catalog model compatibility checked: modelId=${modelId.value}, state=${compatibility.state}, reasons=${compatibility.reasons.size}")
        require(compatibility.state != ModelCompatibilityState.INCOMPATIBLE) { compatibility.reasons.joinToString() }
        transferState.update { it + (modelId to ModelTransferState.Queued) }
        try {
            ModelTransferScheduler(application).schedule(entry)
            Log.i(TAG, "Catalog model download scheduled: modelId=${modelId.value}")
        } catch (error: Throwable) {
            Log.e(TAG, "Catalog model download scheduling failed: modelId=${modelId.value}, message=${error.message}", error)
            transferState.update { it + (modelId to ModelTransferState.Failed(error.message ?: "Download could not be scheduled.")) }
            throw error
        }
    }

    override suspend fun cancelTransfer(modelId: ModelId) {
        if (transferState.stateFor(modelId) == ModelTransferState.Installing) {
            Log.w(TAG, "Ignoring cancellation during model installation: modelId=${modelId.value}")
            return
        }
        Log.i(TAG, "Catalog model transfer cancellation requested: modelId=${modelId.value}")
        ModelTransferScheduler(application).cancel(modelId)
        transferState.update { it + (modelId to ModelTransferState.Cancelled) }
    }

    override suspend fun executeScheduledDownload(modelId: ModelId): Result<Unit> = runCatching {
        val entry = catalogEntry(modelId)
        Log.i(TAG, "Scheduled catalog model download started: modelId=${modelId.value}")
        if (installedModels.registerInstalledDirectory(modelId)) {
            transferState.update { it + (modelId to ModelTransferState.Completed) }
            return@runCatching
        }
        withContext(Dispatchers.IO) { downloadAndInstall(entry) }
    }

    private suspend fun downloadAndInstall(entry: CatalogModel) {
        val modelId = entry.manifest.modelId
        val temporary = temporaryDirectory(modelId)
        Log.i(TAG, "Catalog model transfer started: modelId=${modelId.value}, stagingDirectory=${temporary.name}")
        transferState.update { it + (modelId to ModelTransferState.Running(0, entry.download.expectedBytes)) }
        try {
            val downloads = entry.download.files.ifEmpty {
                val file = entry.manifest.files.single()
                listOf(CatalogDownloadFile(file.relativePath, requireNotNull(entry.download.url) { "The catalog download URL is missing." }))
            }
            val specifications = entry.manifest.files.associateBy(ModelFileSpec::relativePath)
            val expectedTotal = downloads.sumOf { download ->
                requireNotNull(specifications[download.relativePath]?.expectedBytes) {
                    "The catalog size for ${download.relativePath} is missing."
                }
            }
            require(expectedTotal == entry.download.expectedBytes) { "The catalog's total download size does not match its files." }
            var completedBeforeFile = 0L
            downloads.forEach { download ->
                coroutineContext.ensureActive()
                val specification = requireNotNull(specifications[download.relativePath]) {
                    "The catalog download ${download.relativePath} is not declared by the model."
                }
                val expectedBytes = requireNotNull(specification.expectedBytes) {
                    "The catalog size for ${download.relativePath} is missing."
                }
                val expectedSha256 = requireNotNull(specification.sha256) {
                    "The catalog checksum for ${download.relativePath} is missing."
                }
                val destination = File(temporary, download.relativePath)
                Log.i(TAG, "Catalog model file download started: modelId=${modelId.value}, file=${download.relativePath}, expectedBytes=$expectedBytes")
                require(destination.canonicalPath.startsWith(temporary.canonicalPath + File.separator)) {
                    "The model manifest contains an unsafe path."
                }
                val parent = requireNotNull(destination.parentFile)
                require(parent.isDirectory || parent.mkdirs()) { "Could not prepare the model installation directory." }
                downloadTo(download.url, destination, expectedBytes, modelId, completedBeforeFile, entry.download.expectedBytes)
                require(destination.length() == expectedBytes) {
                    "Downloaded size for ${download.relativePath} does not match the catalog."
                }
                require(destination.sha256().equals(expectedSha256, ignoreCase = true)) {
                    "Downloaded checksum for ${download.relativePath} does not match the catalog."
                }
                Log.i(TAG, "Catalog model file verified: modelId=${modelId.value}, file=${download.relativePath}, bytes=${destination.length()}")
                completedBeforeFile += expectedBytes
            }
            transferState.update { it + (modelId to ModelTransferState.Installing) }
            Log.i(TAG, "Catalog model installation started: modelId=${modelId.value}")
            installedModels.installDirectory(entry.manifest.copy(installedAtEpochMs = System.currentTimeMillis()), temporary, verifyChecksums = false)
            transferState.update { it + (modelId to ModelTransferState.Completed) }
            Log.i(TAG, "Catalog model transfer completed: modelId=${modelId.value}")
        } catch (cancelled: CancellationException) {
            Log.i(TAG, "Catalog model transfer cancelled: modelId=${modelId.value}")
            temporary.deleteRecursively()
            transferState.update { it + (modelId to ModelTransferState.Cancelled) }
            throw cancelled
        } catch (error: Throwable) {
            Log.e(TAG, "Catalog model transfer failed: modelId=${modelId.value}, message=${error.message}", error)
            temporary.deleteRecursively()
            transferState.update { it + (modelId to ModelTransferState.Failed(error.message ?: "Download failed.")) }
            throw error
        }
    }

    private suspend fun downloadTo(
        url: String,
        destination: File,
        expectedBytes: Long,
        modelId: ModelId,
        completedBeforeFile: Long,
        totalBytes: Long,
    ) {
        var currentUrl = url
        repeat(5) {
            coroutineContext.ensureActive()
            val connection = (URI(currentUrl).toURL().openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = requireNotNull(connection.getHeaderField("Location")) { "Redirect without a target." }
                    currentUrl = URI(currentUrl).resolve(location).toString()
                    Log.i(TAG, "Catalog model download redirected: modelId=${modelId.value}, redirect=${it + 1}")
                    return@repeat
                }
                require(status in 200..299) { "Download failed with HTTP $status." }
                Log.i(TAG, "Catalog model HTTP response accepted: modelId=${modelId.value}, status=$status")
                connection.getHeaderFieldLong("Content-Length", -1).takeIf { it >= 0 }?.let { announced ->
                    require(announced == expectedBytes) { "Server response length does not match the catalog." }
                }
                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var completed = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            completed += read
                            transferState.update { states ->
                                states + (modelId to ModelTransferState.Running(completedBeforeFile + completed, totalBytes))
                            }
                        }
                    }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        error("The download redirected too many times.")
    }

    private fun catalogEntry(modelId: ModelId) = ModelCatalog.entries.firstOrNull { it.manifest.modelId == modelId }
        ?: error("This catalog model is no longer available in the bundled catalog.")

    private fun temporaryDirectory(modelId: ModelId): File = File(
        installedModels.rootDirectory.parentFile,
        "model-installing-${modelId.value}-${UUID.randomUUID()}",
    ).also { require(it.mkdirs()) { "Could not create the installation staging directory." } }

    private companion object {
        const val TAG = "AiP123Models"
    }
}

internal interface ModelDownloadExecutor {
    suspend fun executeScheduledDownload(modelId: ModelId): Result<Unit>
}
