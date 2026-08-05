package com.dmitriim.localaiplayground.source.models.transfer

import java.io.IOException

internal class ModelDownloadFailure(
    message: String,
    val retryable: Boolean,
) : IOException(message)

internal fun Result<Unit>.shouldRetryDownload(): Boolean =
    (exceptionOrNull() as? ModelDownloadFailure)?.retryable ?: true
