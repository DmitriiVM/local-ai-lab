package com.dmitriim.localailab.core.model.service

/** Initializes persisted model-transfer recovery when the app process starts. */
interface ModelTransferStartup {
    fun initialize()
}
