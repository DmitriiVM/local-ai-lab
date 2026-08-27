package com.dmitriim.localailab.core.model.manifest

import com.dmitriim.localailab.core.model.capability.AiCapability

/** Display-safe metadata for a packaged runtime profile. */
data class ModelProfileDescriptor(
    val key: ModelProfileKey,
    val displayName: String,
    val capabilities: Set<AiCapability>,
    val importable: Boolean,
)
