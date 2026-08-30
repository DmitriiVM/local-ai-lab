package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey

/** Resolves the runtime profile that supports a persisted engine/profile key. */
fun interface ModelRuntimeProfileResolver {
    fun runtimeProfile(key: ModelProfileKey): ModelRuntimeProfile?

    fun requireRuntimeProfile(key: ModelProfileKey): ModelRuntimeProfile = requireNotNull(runtimeProfile(key)) {
        "No packaged runtime profile supports ${key.engineId.value}/${key.profileId.value}."
    }
}
