package com.dmitriim.localailab.feature.models.impl.domain.transfer

/** Defines bounded backoff for resumable catalog-download retries. */
internal object ModelDownloadRetryPolicy {
    fun delayMillis(retryAttempt: Int, minimumDelayMillis: Long = 0L): Long? {
        val backoff = RETRY_DELAYS_MS.getOrNull(retryAttempt) ?: return null
        return maxOf(backoff, minimumDelayMillis)
    }

    private val RETRY_DELAYS_MS = longArrayOf(30_000L, 60_000L, 120_000L, 300_000L, 600_000L)
}
