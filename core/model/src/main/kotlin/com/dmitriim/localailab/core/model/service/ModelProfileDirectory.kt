package com.dmitriim.localailab.core.model.service

import com.dmitriim.localailab.core.model.manifest.ModelProfileDescriptor
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey

/** Read-only profile metadata available to model-management features. */
interface ModelProfileDirectory {
    val profiles: List<ModelProfileDescriptor>
    fun find(key: ModelProfileKey): ModelProfileDescriptor?
}
