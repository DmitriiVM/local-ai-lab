package com.dmitriim.localailab.feature.models.impl.data.transfer

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import com.dmitriim.localailab.core.model.manifest.ModelId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ModelDownloadJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningJobs = mutableMapOf<Int, Job>()

    override fun onStartJob(params: JobParameters): Boolean {
        val modelId = params.extras.getString(MODEL_ID_KEY) ?: return false
        val executionGeneration = params.extras.getLong(MODEL_TRANSFER_GENERATION_KEY, -1L)
        if (executionGeneration < 0) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setNotification(
                params,
                MODEL_DOWNLOAD_NOTIFICATION_ID,
                modelDownloadNotification(this, "Downloading model"),
                JobService.JOB_END_NOTIFICATION_POLICY_DETACH,
            )
        }
        val jobId = params.jobId
        val job = scope.launch {
            try {
                ModelDownloadRuntime.executor?.executeScheduledDownload(ModelId(modelId), executionGeneration)
                jobFinished(params, false)
            } finally {
                synchronized(runningJobs) { runningJobs.remove(jobId) }
            }
        }
        synchronized(runningJobs) { runningJobs[jobId] = job }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        synchronized(runningJobs) { runningJobs.remove(params.jobId) }?.cancel()
        return false
    }
}
