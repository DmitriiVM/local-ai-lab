package com.dmitriim.localailab.feature.models.impl.data.transfer

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.models.impl.data.persistence.ModelsDatabaseProvider
import com.dmitriim.localailab.feature.models.impl.data.persistence.ModelTransferEntity
import com.dmitriim.localailab.feature.models.impl.data.persistence.ModelTransferFileEntity
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed transfer state that survives process death and scheduler recreation. */
@Inject
@SingleIn(AppScope::class)
class ModelTransferStateStore(
    databaseProvider: ModelsDatabaseProvider,
) {
    private val dao = databaseProvider.database.modelTransferDao()
    private val fileDao = databaseProvider.database.modelTransferFileDao()

    val transfers: Flow<Map<ModelId, ModelTransferState>> = dao.observeAll().map { transfers ->
        transfers.associate { transfer -> ModelId(transfer.modelId) to transfer.toState() }
    }

    internal suspend fun find(modelId: ModelId): StoredModelTransfer? = dao.find(modelId.value)?.toStored()

    internal suspend fun all(): List<StoredModelTransfer> = dao.all().map { it.toStored() }

    internal suspend fun queueNew(
        modelId: ModelId,
        catalogVersion: Int,
        revision: String?,
        totalBytes: Long,
        networkPolicy: ModelTransferNetworkPolicy,
    ): StoredModelTransfer {
        val existing = dao.find(modelId.value)
        val now = System.currentTimeMillis()
        val transfer = ModelTransferEntity(
            modelId = modelId.value,
            catalogVersion = catalogVersion,
            revision = revision,
            status = PersistedModelTransferStatus.QUEUED.name,
            networkPolicy = networkPolicy.name,
            executionGeneration = (existing?.executionGeneration ?: 0L) + 1L,
            completedBytes = existing?.completedBytes ?: 0L,
            totalBytes = totalBytes,
            currentRelativePath = existing?.currentRelativePath,
            message = null,
            retryAttempt = 0,
            nextAttemptAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        dao.upsert(transfer)
        return transfer.toStored()
    }

    internal suspend fun claimQueuedWhenNoTransferIsActive(modelId: ModelId, executionGeneration: Long): Boolean = dao.claimQueuedWhenNoTransferIsActive(
        modelId = modelId.value,
        executionGeneration = executionGeneration,
        queuedStatus = PersistedModelTransferStatus.QUEUED.name,
        runningStatus = PersistedModelTransferStatus.RUNNING.name,
        installingStatus = PersistedModelTransferStatus.INSTALLING.name,
        updatedAtEpochMs = System.currentTimeMillis(),
    ) == 1

    internal suspend fun hasActiveTransfer(): Boolean = dao.hasActiveTransfer(
        runningStatus = PersistedModelTransferStatus.RUNNING.name,
        installingStatus = PersistedModelTransferStatus.INSTALLING.name,
    )

    internal suspend fun nextQueued(): StoredModelTransfer? = dao.nextQueued(PersistedModelTransferStatus.QUEUED.name, System.currentTimeMillis())?.toStored()

    internal suspend fun scheduleRetry(transfer: StoredModelTransfer, delayMillis: Long): StoredModelTransfer {
        val now = System.currentTimeMillis()
        return update(
            transfer,
            status = PersistedModelTransferStatus.QUEUED,
            message = "Retrying automatically.",
            retryAttempt = transfer.retryAttempt + 1,
            nextAttemptAtEpochMs = now.saturatingAdd(delayMillis),
        )
    }

    internal suspend fun update(
        transfer: StoredModelTransfer,
        status: PersistedModelTransferStatus = transfer.status,
        completedBytes: Long = transfer.completedBytes,
        currentRelativePath: String? = transfer.currentRelativePath,
        message: String? = transfer.message,
        retryAttempt: Int = transfer.retryAttempt,
        nextAttemptAtEpochMs: Long = transfer.nextAttemptAtEpochMs,
    ): StoredModelTransfer {
        val updated = ModelTransferEntity(
            modelId = transfer.modelId,
            catalogVersion = transfer.catalogVersion,
            revision = transfer.revision,
            status = status.name,
            networkPolicy = transfer.networkPolicy.name,
            executionGeneration = transfer.executionGeneration,
            completedBytes = completedBytes,
            totalBytes = transfer.totalBytes,
            currentRelativePath = currentRelativePath,
            message = message,
            retryAttempt = retryAttempt,
            nextAttemptAtEpochMs = nextAttemptAtEpochMs,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        dao.upsert(updated)
        return updated.toStored()
    }

    internal suspend fun updateWhileRunning(
        transfer: StoredModelTransfer,
        status: PersistedModelTransferStatus = PersistedModelTransferStatus.RUNNING,
        completedBytes: Long = transfer.completedBytes,
        currentRelativePath: String? = transfer.currentRelativePath,
        message: String? = transfer.message,
    ): StoredModelTransfer? {
        val updated = dao.updateWhileRunning(
            modelId = transfer.modelId,
            executionGeneration = transfer.executionGeneration,
            runningStatus = PersistedModelTransferStatus.RUNNING.name,
            status = status.name,
            completedBytes = completedBytes,
            currentRelativePath = currentRelativePath,
            message = message,
            updatedAtEpochMs = System.currentTimeMillis(),
        ) == 1
        return if (updated) {
            transfer.copy(
                status = status,
                completedBytes = completedBytes,
                currentRelativePath = currentRelativePath,
                message = message,
            )
        } else {
            null
        }
    }

    internal suspend fun updateFileValidator(
        modelId: ModelId,
        relativePath: String,
        eTag: String?,
        lastModified: String?,
        verified: Boolean,
    ) {
        fileDao.upsert(
            ModelTransferFileEntity(
                modelId = modelId.value,
                relativePath = relativePath,
                eTag = eTag,
                lastModified = lastModified,
                verified = verified,
            ),
        )
    }

    internal suspend fun markFileVerified(modelId: ModelId, relativePath: String) {
        val current = fileDao.filesFor(modelId.value).firstOrNull { it.relativePath == relativePath }
            ?: ModelTransferFileEntity(modelId.value, relativePath, null, null, verified = false)
        fileDao.upsert(current.copy(verified = true))
    }

    internal suspend fun fileValidators(modelId: ModelId): Map<String, ModelTransferFileEntity> = fileDao.filesFor(modelId.value).associateBy(ModelTransferFileEntity::relativePath)

    suspend fun delete(modelId: ModelId) = dao.deleteTransfer(modelId.value)

    private fun ModelTransferEntity.toStored() = StoredModelTransfer(
        modelId = modelId,
        catalogVersion = catalogVersion,
        revision = revision,
        status = PersistedModelTransferStatus.valueOf(status),
        networkPolicy = ModelTransferNetworkPolicy.valueOf(networkPolicy),
        executionGeneration = executionGeneration,
        completedBytes = completedBytes,
        totalBytes = totalBytes,
        currentRelativePath = currentRelativePath,
        message = message,
        retryAttempt = retryAttempt,
        nextAttemptAtEpochMs = nextAttemptAtEpochMs,
    )

    private fun Long.saturatingAdd(other: Long): Long = if (this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other

    private fun ModelTransferEntity.toState(): ModelTransferState = when (PersistedModelTransferStatus.valueOf(status)) {
        PersistedModelTransferStatus.QUEUED -> ModelTransferState.Queued(
            completedBytes = completedBytes,
            totalBytes = totalBytes,
            networkPolicy = ModelTransferNetworkPolicy.valueOf(networkPolicy),
        )
        PersistedModelTransferStatus.RUNNING -> ModelTransferState.Running(
            completedBytes = completedBytes,
            totalBytes = totalBytes,
            networkPolicy = ModelTransferNetworkPolicy.valueOf(networkPolicy),
        )
        PersistedModelTransferStatus.PAUSED -> ModelTransferState.Paused(
            completedBytes = completedBytes,
            totalBytes = totalBytes,
            networkPolicy = ModelTransferNetworkPolicy.valueOf(networkPolicy),
            reason = message,
        )
        PersistedModelTransferStatus.INSTALLING -> ModelTransferState.Installing
        PersistedModelTransferStatus.FAILED -> ModelTransferState.Failed(message ?: "Download failed.")
    }
}
