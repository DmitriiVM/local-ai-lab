package com.dmitriim.localailab.source.models.transfer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dmitriim.localailab.source.models.R

internal fun modelDownloadNotification(context: Context, title: String): Notification {
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
        .setContentText(context.getString(R.string.model_download_notification_content))
        .setOngoing(true)
        .build()
}

internal const val MODEL_DOWNLOAD_NOTIFICATION_ID = 4102
private const val MODEL_DOWNLOAD_CHANNEL_ID = "model-downloads"
