package com.dmitriim.localailab.feature.models.impl.models.data.persistence

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Persists resumable-download validators for individual files within a model transfer. */
@Dao
interface ModelTransferFileDao {
    @Query("SELECT * FROM model_transfer_files WHERE modelId = :modelId")
    suspend fun filesFor(modelId: String): List<ModelTransferFileEntity>

    @Upsert
    suspend fun upsert(entity: ModelTransferFileEntity)
}
