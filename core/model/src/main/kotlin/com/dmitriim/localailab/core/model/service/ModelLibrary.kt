package com.dmitriim.localailab.core.model.service

import com.dmitriim.localailab.core.model.library.InstalledModel
import com.dmitriim.localailab.core.model.library.ModelImportRequest
import com.dmitriim.localailab.core.model.manifest.ModelId
import kotlinx.coroutines.flow.Flow

/** Owns the user-visible library of installed local models. */
interface ModelLibrary {
    val installedModels: Flow<List<InstalledModel>>

    suspend fun import(request: ModelImportRequest): Result<ModelId>
    suspend fun validate(modelId: ModelId): Result<InstalledModel>
    suspend fun delete(modelId: ModelId): Result<Unit>
}
