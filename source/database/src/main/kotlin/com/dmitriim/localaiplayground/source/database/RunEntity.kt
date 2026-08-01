package com.dmitriim.localaiplayground.source.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "runs",
    indices = [Index("completedAtEpochMs"), Index("capability"), Index("status"), Index("modelId")],
)
data class RunEntity(
    @PrimaryKey val id: String,
    val capability: String,
    val status: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val modelId: String?,
    val modelDisplayName: String?,
    val engineId: String?,
    val modelRevision: String?,
    val input: String?,
    val output: String?,
    val parametersJson: String,
    val metricsJson: String,
    val errorMessage: String?,
    val linkedRunIdsJson: String,
)
