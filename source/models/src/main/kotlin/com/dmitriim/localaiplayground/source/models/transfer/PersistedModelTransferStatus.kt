package com.dmitriim.localaiplayground.source.models.transfer

internal enum class PersistedModelTransferStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    INSTALLING,
    FAILED,
}
