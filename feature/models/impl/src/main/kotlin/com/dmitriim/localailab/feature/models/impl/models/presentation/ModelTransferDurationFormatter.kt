package com.dmitriim.localailab.feature.models.impl.models.presentation

internal fun Long.toModelTransferRemainingDuration(): String {
    val remainingSeconds = (coerceAtLeast(0L) + 999L) / 1_000L
    return when {
        remainingSeconds < 60L -> "< 1 min"
        remainingSeconds < 3_600L -> "${(remainingSeconds + 59L) / 60L} min"
        else -> {
            val hours = remainingSeconds / 3_600L
            val minutes = (remainingSeconds % 3_600L + 59L) / 60L
            if (minutes == 60L) "${hours + 1L} h" else "$hours h $minutes min"
        }
    }
}
