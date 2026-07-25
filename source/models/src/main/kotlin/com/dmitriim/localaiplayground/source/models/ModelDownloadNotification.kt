package com.dmitriim.localaiplayground.source.models

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

internal fun modelDownloadNotification(context: Context, title: String): Notification {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(MODEL_DOWNLOAD_CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }
    return NotificationCompat.Builder(context, MODEL_DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setContentText("The installation is verified before it becomes available.")
        .setOngoing(true)
        .build()
}

internal const val MODEL_DOWNLOAD_NOTIFICATION_ID = 4102
private const val MODEL_DOWNLOAD_CHANNEL_ID = "model-downloads"
