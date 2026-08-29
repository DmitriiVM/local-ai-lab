package com.dmitriim.localailab.core.navigation.destination

import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.navigation.AppDestination
import kotlinx.serialization.Serializable

@Serializable
data class ModelDetailsDestination(
    val modelId: ModelId,
) : AppDestination
