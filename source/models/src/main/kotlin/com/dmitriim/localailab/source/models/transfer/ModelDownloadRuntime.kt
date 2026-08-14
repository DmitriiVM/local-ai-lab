package com.dmitriim.localailab.source.models.transfer

/** Process-local bridge initialized by the app graph at process startup. */
internal object ModelDownloadRuntime {
    @Volatile var executor: ModelDownloadExecutor? = null
}
