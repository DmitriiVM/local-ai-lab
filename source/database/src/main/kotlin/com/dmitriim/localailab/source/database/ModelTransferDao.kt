package com.dmitriim.localailab.source.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelTransferDao {
    @Query("SELECT * FROM model_transfers ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ModelTransferEntity>>

    @Query("SELECT * FROM model_transfers WHERE modelId = :modelId LIMIT 1")
    suspend fun find(modelId: String): ModelTransferEntity?

    @Query("SELECT * FROM model_transfers")
    suspend fun all(): List<ModelTransferEntity>

    @Upsert
    suspend fun upsert(entity: ModelTransferEntity)

    @Query(
        "UPDATE model_transfers SET status = :runningStatus, updatedAtEpochMs = :updatedAtEpochMs " +
            "WHERE modelId = :modelId AND status = :queuedStatus AND executionGeneration = :executionGeneration",
    )
    suspend fun claimQueued(
        modelId: String,
        executionGeneration: Long,
        queuedStatus: String,
        runningStatus: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        "UPDATE model_transfers SET status = :status, completedBytes = :completedBytes, " +
            "currentRelativePath = :currentRelativePath, message = :message, updatedAtEpochMs = :updatedAtEpochMs " +
            "WHERE modelId = :modelId AND executionGeneration = :executionGeneration AND status = :runningStatus",
    )
    suspend fun updateWhileRunning(
        modelId: String,
        executionGeneration: Long,
        runningStatus: String,
        status: String,
        completedBytes: Long,
        currentRelativePath: String?,
        message: String?,
        updatedAtEpochMs: Long,
    ): Int

    @Query("DELETE FROM model_transfers WHERE modelId = :modelId")
    suspend fun delete(modelId: String)

    @Query("SELECT * FROM model_transfer_files WHERE modelId = :modelId")
    suspend fun filesFor(modelId: String): List<ModelTransferFileEntity>

    @Upsert
    suspend fun upsertFile(entity: ModelTransferFileEntity)

    @Query("DELETE FROM model_transfer_files WHERE modelId = :modelId")
    suspend fun deleteFiles(modelId: String)

    @Transaction
    suspend fun deleteTransfer(modelId: String) {
        deleteFiles(modelId)
        delete(modelId)
    }
}
