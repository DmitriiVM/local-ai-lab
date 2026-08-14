package com.dmitriim.localailab.core.model.manifest

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ModelId(val value: String)

@Serializable
enum class ModelLifecycleState {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    LOADED,
    INVALID,
}
