package com.dmitriim.localailab.feature.models.navigation

import androidx.navigation3.runtime.NavKey
import com.dmitriim.localailab.core.model.manifest.ModelId
import kotlinx.serialization.Serializable

@Serializable
data object ModelsKey : NavKey

@Serializable
data class ModelDetailsKey(val modelId: ModelId) : NavKey
