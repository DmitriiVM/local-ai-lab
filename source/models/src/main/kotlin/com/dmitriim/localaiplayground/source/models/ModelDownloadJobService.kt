package com.dmitriim.localaiplayground.source.models

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import com.dmitriim.localaiplayground.core.model.ModelId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ModelDownloadJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runningJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val modelId = params.extras.getString(MODEL_ID_KEY) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setNotification(
                params,
                MODEL_DOWNLOAD_NOTIFICATION_ID,
                modelDownloadNotification(this, "Downloading model"),
                JobService.JOB_END_NOTIFICATION_POLICY_DETACH,
            )
        }
        runningJob = scope.launch {
            val success = ModelDownloadRuntime.repository
                ?.executeScheduledDownload(ModelId(modelId))
                ?.isSuccess == true
            jobFinished(params, !success)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        runningJob?.cancel(CancellationException("Android stopped the download"))
        return true
    }
}
