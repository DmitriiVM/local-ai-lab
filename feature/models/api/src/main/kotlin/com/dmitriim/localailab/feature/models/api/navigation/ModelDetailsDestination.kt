package com.dmitriim.localailab.feature.models.api.navigation

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.navigation.AppDestination
import kotlinx.serialization.Serializable

@Serializable
data class ModelDetailsDestination(
    val modelId: ModelId,
) : AppDestination
