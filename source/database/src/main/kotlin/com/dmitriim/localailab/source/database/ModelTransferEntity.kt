package com.dmitriim.localailab.source.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Durable state for one catalog model download that has not been installed yet. */
@Entity(tableName = "model_transfers")
data class ModelTransferEntity(
    @PrimaryKey val modelId: String,
    val catalogVersion: Int,
    val revision: String?,
    val status: String,
    val networkPolicy: String,
    val executionGeneration: Long,
    val completedBytes: Long,
    val totalBytes: Long,
    val currentRelativePath: String?,
    val message: String?,
    val retryAttempt: Int,
    val nextAttemptAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
