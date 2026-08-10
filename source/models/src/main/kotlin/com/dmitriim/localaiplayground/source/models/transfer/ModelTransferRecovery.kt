package com.dmitriim.localaiplayground.source.models.transfer

import android.app.Application
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.source.models.catalog.ModelCatalog
import com.dmitriim.localaiplayground.source.models.library.InstalledModelService
import com.dmitriim.localaiplayground.source.models.library.ModelImportPolicy
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
            if (transfer.status == PersistedModelTransferStatus.RUNNING || transfer.status == PersistedModelTransferStatus.INSTALLING) {
                transferState.update(
                    transfer,
                    status = PersistedModelTransferStatus.PAUSED,
                    message = "Download interrupted. Tap Resume to continue.",
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
