package com.dmitriim.localailab.feature.models.impl.data.transfer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dmitriim.localailab.feature.models.impl.R
import java.util.Locale

internal fun modelDownloadNotification(
    context: Context,
    title: String,
    completedBytes: Long? = null,
    totalBytes: Long? = null,
): Notification {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                MODEL_DOWNLOAD_CHANNEL_ID,
                context.getString(R.string.model_download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }
    return NotificationCompat.Builder(context, MODEL_DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .apply {
            if (completedBytes != null && totalBytes != null && totalBytes > 0L) {
                setContentText(
                    context.getString(
                        R.string.model_download_notification_progress,
                        completedBytes.toReadableNotificationBytes(),
                        totalBytes.toReadableNotificationBytes(),
                    ),
                )
                setProgress(
                    NOTIFICATION_PROGRESS_MAX,
                    ((completedBytes.coerceIn(0L, totalBytes).toDouble() / totalBytes) * NOTIFICATION_PROGRESS_MAX).toInt(),
                    false,
                )
            } else {
                setContentText(context.getString(R.string.model_download_notification_content))
                setProgress(0, 0, true)
            }
        }
        .build()
}

internal fun updateModelDownloadNotification(context: Context, completedBytes: Long, totalBytes: Long) {
    context.getSystemService(NotificationManager::class.java).notify(
        MODEL_DOWNLOAD_NOTIFICATION_ID,
        modelDownloadNotification(context, "Downloading model", completedBytes, totalBytes),
    )
}

private fun Long.toReadableNotificationBytes(): String = when {
    this >= GIBIBYTE -> String.format(Locale.US, "%.1f GiB", toDouble() / GIBIBYTE)
    this >= MEBIBYTE -> String.format(Locale.US, "%.0f MiB", toDouble() / MEBIBYTE)
    else -> String.format(Locale.US, "%.0f KiB", toDouble() / KIBIBYTE)
}

internal const val MODEL_DOWNLOAD_NOTIFICATION_ID = 4102
private const val MODEL_DOWNLOAD_CHANNEL_ID = "model-downloads"
private const val NOTIFICATION_PROGRESS_MAX = 1_000
private const val KIBIBYTE = 1024L
private const val MEBIBYTE = KIBIBYTE * 1024
private const val GIBIBYTE = MEBIBYTE * 1024
