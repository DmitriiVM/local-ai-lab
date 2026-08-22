package com.dmitriim.localailab.source.models.transfer

import android.app.Application
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.source.models.catalog.ModelCatalog
import com.dmitriim.localailab.source.models.library.InstalledModelService
import com.dmitriim.localailab.source.models.library.ModelImportPolicy
import java.io.File

/** Reconciles persisted download state and staging files after process recreation. */
internal class ModelTransferRecovery(
    private val application: Application,
    private val installedModels: InstalledModelService,
    private val transferState: ModelTransferStateStore,
) {
    suspend fun reconcile() {
        val activeIds = mutableSetOf<String>()
        transferState.all().forEach { transfer ->
            val modelId = ModelId(transfer.modelId)
            val entry = ModelCatalog.entries.firstOrNull { it.manifest.modelId == modelId }
            if (entry == null || entry.manifest.catalogVersion != transfer.catalogVersion || entry.manifest.revision != transfer.revision) {
                stagingDirectory(modelId).deleteRecursively()
                transferState.delete(modelId)
                return@forEach
            }
            if (installedModels.registerInstalledDirectory(modelId)) {
                transferState.delete(modelId)
                return@forEach
            }
            activeIds += ModelImportPolicy.directoryName(modelId)
            if (transfer.status == PersistedModelTransferStatus.RUNNING) {
                val retryDelay = ModelDownloadRetryPolicy.delayMillis(transfer.retryAttempt)
                if (retryDelay == null) {
                    transferState.update(
                        transfer,
                        status = PersistedModelTransferStatus.PAUSED,
                        message = "Automatic retries exhausted. Tap Resume to continue.",
                    )
                } else {
                    transferState.scheduleRetry(transfer, retryDelay)
                }
                ModelTransferScheduler(application).cancel(modelId)
            } else if (transfer.status == PersistedModelTransferStatus.INSTALLING) {
                transferState.update(
                    transfer,
                    status = PersistedModelTransferStatus.PAUSED,
                    message = "Installation interrupted. Tap Resume to continue.",
                )
                ModelTransferScheduler(application).cancel(modelId)
            }
        }
        stagingRoot().listFiles { file -> file.isDirectory && file.name !in activeIds }
            ?.forEach(File::deleteRecursively)
    }

    private fun stagingRoot(): File = File(application.filesDir, STAGING_DIRECTORY_NAME)

    private fun stagingDirectory(modelId: ModelId): File = File(stagingRoot(), ModelImportPolicy.directoryName(modelId))

    private companion object {
        const val STAGING_DIRECTORY_NAME = "model-downloads"
    }
}
