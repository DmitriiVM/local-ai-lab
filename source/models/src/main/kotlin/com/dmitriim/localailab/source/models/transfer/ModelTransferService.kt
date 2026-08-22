package com.dmitriim.localailab.source.models.transfer

import android.app.Application
import android.util.Log
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.di.ApplicationCoroutineScope
import com.dmitriim.localailab.core.model.library.CatalogDownloadAuthentication
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelCompatibilityState
import com.dmitriim.localailab.core.model.library.ModelTransferNetworkPolicy
import com.dmitriim.localailab.core.model.library.ModelTransferState
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.service.ModelTransfers
import com.dmitriim.localailab.source.models.catalog.ModelCatalog
import com.dmitriim.localailab.source.models.credentials.HuggingFaceTokenStore
import com.dmitriim.localailab.source.models.diagnostics.ModelDiagnosticsService
import com.dmitriim.localailab.source.models.library.InstalledModelService
import com.dmitriim.localailab.source.models.library.ModelImportPolicy
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Owns persisted model download state, resumable transfer, and transactional installation. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ModelTransfers>())
@Suppress("LargeClass") // This service owns the cohesive model-transfer lifecycle.
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
    private val throughputEstimator = ModelTransferThroughputEstimator()
    override val transfers: Flow<Map<ModelId, ModelTransferState>> = combine(
        transferState.transfers,
        throughputEstimator.estimates,
    ) { transfers, throughput ->
        transfers.mapValues { (modelId, state) ->
            if (state is ModelTransferState.Running) {
                throughput[modelId]?.let { estimate ->
                    state.copy(
                        bytesPerSecond = estimate.bytesPerSecond,
                        estimatedRemainingMillis = estimate.estimatedRemainingMillis,
                    )
                } ?: state
            } else {
                state
            }
        }
    }
    private val transferSchedulingMutex = Mutex()
    private val transferScheduler = ModelTransferScheduler(application)
    private val fileDownloader = ModelFileDownloader(transferState, ::ensureRunning, ::publishDownloadProgress)
    private val transferExecution = ModelTransferExecution(
        installedModels,
        transferState,
        fileDownloader,
        ::ensureRunning,
        ::publishDownloadProgress,
    )

    init {
        ModelDownloadRuntime.executor = this
        applicationScope.launch(Dispatchers.IO) {
            transferSchedulingMutex.withLock {
                ModelTransferRecovery(application, installedModels, transferState).reconcile()
                scheduleDeferredRetriesLocked()
                scheduleNextQueuedLocked()
            }
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
        transferSchedulingMutex.withLock {
            val transfer = transferState.find(modelId) ?: return@withLock
            if (transfer.status == PersistedModelTransferStatus.INSTALLING) return@withLock
            val wasRunning = transfer.status == PersistedModelTransferStatus.RUNNING
            transferState.update(
                transfer,
                status = PersistedModelTransferStatus.PAUSED,
                message = "Paused by user.",
            )
            throughputEstimator.clear(modelId)
            transferScheduler.cancel(modelId)
            if (!wasRunning) scheduleNextQueuedLocked()
        }
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
        transferSchedulingMutex.withLock {
            val transfer = transferState.find(modelId) ?: return@withLock
            if (transfer.status == PersistedModelTransferStatus.INSTALLING) return@withLock
            val wasRunning = transfer.status == PersistedModelTransferStatus.RUNNING
            transferState.delete(modelId)
            throughputEstimator.clear(modelId)
            transferScheduler.cancel(modelId)
            stagingDirectory(modelId).deleteRecursively()
            if (!wasRunning) scheduleNextQueuedLocked()
        }
    }

    override suspend fun executeScheduledDownload(modelId: ModelId, executionGeneration: Long) {
        val claimed = transferSchedulingMutex.withLock {
            if (!transferState.claimQueuedWhenNoTransferIsActive(modelId, executionGeneration)) {
                null
            } else {
                requireNotNull(transferState.find(modelId))
            }
        } ?: return
        try {
            publishDownloadProgress(claimed)
            val entry = catalogEntry(modelId)
            require(claimed.catalogVersion == requireNotNull(entry.manifest.catalogVersion) && claimed.revision == entry.manifest.revision) {
                "The bundled catalog changed. Restart this download."
            }
            withContext(Dispatchers.IO) {
                transferExecution.downloadAndInstall(entry, claimed, stagingDirectory(modelId), credentialFor(entry))
            }
        } catch (cancelled: CancellationException) {
            val latest = transferState.find(modelId)
            if (latest?.status == PersistedModelTransferStatus.RUNNING && latest.executionGeneration == executionGeneration) {
                handleDownloadFailure(
                    modelId,
                    executionGeneration,
                    ModelDownloadFailure("Download interrupted.", retryable = true),
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            handleDownloadFailure(modelId, executionGeneration, error)
            Log.e(TAG, "Catalog model transfer failed: modelId=${modelId.value}", error)
        } finally {
            throughputEstimator.clear(modelId)
            transferSchedulingMutex.withLock { scheduleNextQueuedLocked() }
        }
    }

    private suspend fun queue(entry: CatalogModel, networkPolicy: ModelTransferNetworkPolicy) {
        transferSchedulingMutex.withLock {
            val modelId = entry.manifest.modelId
            val stagingDirectory = stagingDirectory(modelId)
            require(stagingDirectory.mkdirs() || stagingDirectory.isDirectory) {
                "Could not prepare the download directory."
            }
            ModelDownloadStoragePreflight(application.filesDir).requireSpaceFor(entry, stagingDirectory)
            transferState.queueNew(
                modelId = modelId,
                catalogVersion = requireNotNull(entry.manifest.catalogVersion),
                revision = entry.manifest.revision,
                totalBytes = entry.download.expectedBytes,
                networkPolicy = networkPolicy,
            )
            scheduleNextQueuedLocked()
        }
    }

    private suspend fun scheduleNextQueuedLocked() {
        while (!transferState.hasActiveTransfer()) {
            val transfer = transferState.nextQueued() ?: return
            try {
                transferScheduler.schedule(
                    catalogEntry(transfer.modelIdAsModelId()),
                    transfer.executionGeneration,
                    transfer.networkPolicy,
                )
                return
            } catch (error: Throwable) {
                transferState.update(
                    transfer,
                    status = PersistedModelTransferStatus.FAILED,
                    message = error.message ?: "Download could not be scheduled.",
                )
            }
        }
    }

    private suspend fun handleDownloadFailure(modelId: ModelId, executionGeneration: Long, error: Throwable) {
        transferSchedulingMutex.withLock {
            val transfer = transferState.find(modelId) ?: return@withLock
            if (transfer.status != PersistedModelTransferStatus.RUNNING || transfer.executionGeneration != executionGeneration) {
                return@withLock
            }
            throughputEstimator.clear(modelId)
            if (!error.isRetryableDownloadFailure()) {
                transferState.update(
                    transfer,
                    status = PersistedModelTransferStatus.FAILED,
                    message = error.message ?: "Download failed.",
                )
                return@withLock
            }
            val delayMillis = ModelDownloadRetryPolicy.delayMillis(
                retryAttempt = transfer.retryAttempt,
                minimumDelayMillis = (error as? ModelDownloadFailure)?.retryAfterMillis ?: 0L,
            )
            if (delayMillis == null) {
                transferState.update(
                    transfer,
                    status = PersistedModelTransferStatus.PAUSED,
                    message = "Automatic retries exhausted. Tap Resume to continue.",
                )
                return@withLock
            }
            val retry = transferState.scheduleRetry(transfer, delayMillis)
            try {
                transferScheduler.scheduleRetry(
                    modelId = modelId,
                    executionGeneration = retry.executionGeneration,
                    networkPolicy = retry.networkPolicy,
                    delayMillis = delayMillis,
                )
            } catch (schedulingError: Throwable) {
                transferState.update(
                    retry,
                    status = PersistedModelTransferStatus.PAUSED,
                    message = schedulingError.message ?: "Automatic retry could not be scheduled. Tap Resume to continue.",
                )
            }
        }
    }

    private suspend fun scheduleDeferredRetriesLocked() {
        val now = System.currentTimeMillis()
        transferState.all()
            .filter { it.status == PersistedModelTransferStatus.QUEUED && it.nextAttemptAtEpochMs > now }
            .forEach { transfer ->
                try {
                    transferScheduler.scheduleRetry(
                        modelId = transfer.modelIdAsModelId(),
                        executionGeneration = transfer.executionGeneration,
                        networkPolicy = transfer.networkPolicy,
                        delayMillis = transfer.nextAttemptAtEpochMs - now,
                    )
                } catch (error: Throwable) {
                    transferState.update(
                        transfer,
                        status = PersistedModelTransferStatus.PAUSED,
                        message = error.message ?: "Automatic retry could not be scheduled. Tap Resume to continue.",
                    )
                }
            }
    }

    private fun publishDownloadProgress(transfer: StoredModelTransfer) {
        throughputEstimator.record(transfer)
        updateModelDownloadNotification(application, transfer.completedBytes, transfer.totalBytes)
    }

    private suspend fun ensureRunning(modelId: ModelId, generation: Long) {
        coroutineContext.ensureActive()
        val transfer = transferState.find(modelId)
        if (transfer?.status != PersistedModelTransferStatus.RUNNING || transfer.executionGeneration != generation) {
            throw CancellationException("The download is no longer active.")
        }
    }

    private fun stagingRoot(): File = File(application.filesDir, STAGING_DIRECTORY_NAME)

    private fun stagingDirectory(modelId: ModelId): File = File(stagingRoot(), ModelImportPolicy.directoryName(modelId))

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

    private companion object {
        const val TAG = "AiP123Models"
        const val STAGING_DIRECTORY_NAME = "model-downloads"
    }
}

internal interface ModelDownloadExecutor {
    suspend fun executeScheduledDownload(modelId: ModelId, executionGeneration: Long)
}
