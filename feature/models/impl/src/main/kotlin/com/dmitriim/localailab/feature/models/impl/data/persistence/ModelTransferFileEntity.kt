package com.dmitriim.localailab.feature.models.impl.data.persistence

import androidx.room.Entity

/** HTTP validators for one file inside a durable catalog model transfer. */
@Entity(
    tableName = "model_transfer_files",
    primaryKeys = ["modelId", "relativePath"],
)
data class ModelTransferFileEntity(
    val modelId: String,
    val relativePath: String,
    val eTag: String?,
    val lastModified: String?,
    val verified: Boolean,
)
