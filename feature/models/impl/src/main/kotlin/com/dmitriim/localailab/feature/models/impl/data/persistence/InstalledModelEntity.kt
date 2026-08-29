package com.dmitriim.localailab.feature.models.impl.data.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_models")
data class InstalledModelEntity(
    @PrimaryKey val modelId: String,
    val manifestJson: String,
    val localDirectoryName: String,
    val totalBytes: Long,
    val validationState: String,
    val validationMessage: String?,
    val lastUsedAtEpochMs: Long?,
)
