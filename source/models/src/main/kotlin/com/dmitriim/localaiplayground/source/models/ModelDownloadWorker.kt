package com.dmitriim.localaiplayground.source.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dmitriim.localaiplayground.core.model.ModelId

class ModelDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val modelId = inputData.getString(MODEL_ID_KEY) ?: return Result.failure()
        setForeground(
            ForegroundInfo(
                MODEL_DOWNLOAD_NOTIFICATION_ID,
                modelDownloadNotification(applicationContext, "Downloading model"),
            ),
        )
        return ModelDownloadRuntime.repository
            ?.executeScheduledDownload(ModelId(modelId))
            ?.fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
            ?: Result.retry()
    }
}
