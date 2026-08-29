package com.dmitriim.localailab.feature.models.api.domain.transfer

/** Initializes persisted model-transfer recovery when the app process starts. */
interface ModelTransferStartup {
    fun initialize()
}
