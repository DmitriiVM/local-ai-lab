package com.dmitriim.localailab.feature.models.impl.models.domain.transfer

import java.io.IOException

internal class ModelDownloadFailure(
    message: String,
    val retryable: Boolean,
    val retryAfterMillis: Long? = null,
) : IOException(message)
