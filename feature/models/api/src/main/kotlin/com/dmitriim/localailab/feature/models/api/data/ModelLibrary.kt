package com.dmitriim.localailab.feature.models.api.data

import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.core.model.manifest.ModelId
import kotlinx.coroutines.flow.Flow

/** Owns the user-visible library of installed local models. */
interface ModelLibrary {
    val installedModels: Flow<List<InstalledModel>>

    suspend fun validate(modelId: ModelId): Result<InstalledModel>
    suspend fun delete(modelId: ModelId): Result<Unit>
}
