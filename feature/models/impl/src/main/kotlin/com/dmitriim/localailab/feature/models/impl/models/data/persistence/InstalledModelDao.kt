package com.dmitriim.localailab.feature.models.impl.models.data.persistence

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledModelDao {
    @Query("SELECT * FROM installed_models ORDER BY modelId")
    suspend fun all(): List<InstalledModelEntity>

    @Query("SELECT * FROM installed_models ORDER BY modelId")
    fun observeAll(): Flow<List<InstalledModelEntity>>

    @Query("SELECT * FROM installed_models WHERE modelId = :modelId LIMIT 1")
    suspend fun find(modelId: String): InstalledModelEntity?

    @Upsert
    suspend fun upsert(entity: InstalledModelEntity)

    @Query("DELETE FROM installed_models WHERE modelId = :modelId")
    suspend fun delete(modelId: String)
}
