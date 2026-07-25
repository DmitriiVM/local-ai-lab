package com.dmitriim.localaiplayground.source.models

/** Process-local bridge initialized by the app graph at process startup. */
internal object ModelDownloadRuntime {
    @Volatile var repository: ModelRepositoryImpl? = null
}
