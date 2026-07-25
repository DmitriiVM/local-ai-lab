package com.dmitriim.localaiplayground.source.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY completedAtEpochMs DESC")
    fun observeAll(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<RunEntity?>

    @Upsert
    suspend fun upsert(entity: RunEntity)

    @Query("DELETE FROM runs")
    suspend fun clear()
}
