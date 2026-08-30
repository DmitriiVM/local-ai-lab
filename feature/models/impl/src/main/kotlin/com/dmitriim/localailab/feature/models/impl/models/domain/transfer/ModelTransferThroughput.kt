package com.dmitriim.localailab.feature.models.impl.models.domain.transfer

internal data class ModelTransferThroughput(
    val bytesPerSecond: Long,
    val estimatedRemainingMillis: Long,
)
