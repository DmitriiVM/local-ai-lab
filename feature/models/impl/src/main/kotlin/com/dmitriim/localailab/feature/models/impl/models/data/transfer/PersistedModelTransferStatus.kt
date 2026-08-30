package com.dmitriim.localailab.feature.models.impl.models.data.transfer

internal enum class PersistedModelTransferStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    INSTALLING,
    FAILED,
}
