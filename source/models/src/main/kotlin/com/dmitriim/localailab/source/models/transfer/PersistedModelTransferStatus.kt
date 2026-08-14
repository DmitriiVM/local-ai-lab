package com.dmitriim.localailab.source.models.transfer

internal enum class PersistedModelTransferStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    INSTALLING,
    FAILED,
}
