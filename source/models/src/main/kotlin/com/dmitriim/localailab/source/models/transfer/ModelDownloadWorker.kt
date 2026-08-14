package com.dmitriim.localailab.source.models.transfer

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dmitriim.localailab.core.model.manifest.ModelId

class ModelDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val modelId = inputData.getString(MODEL_ID_KEY) ?: return Result.failure()
        val executionGeneration = inputData.getLong(MODEL_TRANSFER_GENERATION_KEY, -1L)
        if (executionGeneration < 0) return Result.failure()
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        setForeground(
            ForegroundInfo(
                MODEL_DOWNLOAD_NOTIFICATION_ID,
                modelDownloadNotification(applicationContext, "Downloading model"),
                foregroundServiceType,
            ),
        )
        ModelDownloadRuntime.executor?.executeScheduledDownload(ModelId(modelId), executionGeneration)
        return Result.success()
    }
}
