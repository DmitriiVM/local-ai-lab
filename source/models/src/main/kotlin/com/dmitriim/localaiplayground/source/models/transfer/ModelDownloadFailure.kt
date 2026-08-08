package com.dmitriim.localaiplayground.source.models.transfer

import java.io.IOException

internal class ModelDownloadFailure(
    message: String,
    val retryable: Boolean,
    val retryAfterMillis: Long? = null,
) : IOException(message)
