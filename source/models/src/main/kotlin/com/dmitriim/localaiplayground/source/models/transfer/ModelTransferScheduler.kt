package com.dmitriim.localaiplayground.source.models.transfer

import android.annotation.TargetApi
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.PersistableBundle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.manifest.ModelId

/** Selects one persisted scheduling mechanism; both invoke the same transactional installer. */
internal class ModelTransferScheduler(
    private val application: Application,
) {
    fun schedule(entry: CatalogModel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            scheduleUserInitiatedTransfer(entry)
        } else {
            scheduleForegroundWorker(entry.manifest.modelId)
        }
    }

    fun cancel(modelId: ModelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            application.getSystemService(JobScheduler::class.java).cancel(jobId(modelId))
        }
        WorkManager.getInstance(application).cancelUniqueWork(workName(modelId))
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun scheduleUserInitiatedTransfer(entry: CatalogModel) {
        val extras = PersistableBundle().apply { putString(MODEL_ID_KEY, entry.manifest.modelId.value) }
        val network = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val job = JobInfo.Builder(jobId(entry.manifest.modelId), ComponentName(application, ModelDownloadJobService::class.java))
            .setRequiredNetwork(network)
            .setEstimatedNetworkBytes(entry.download.expectedBytes, 0)
            .setUserInitiated(true)
            .setExtras(extras)
            .build()
        check(application.getSystemService(JobScheduler::class.java).schedule(job) == JobScheduler.RESULT_SUCCESS) {
            "Android could not schedule this user-initiated download."
        }
    }

    private fun scheduleForegroundWorker(modelId: ModelId) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(MODEL_ID_KEY to modelId.value))
            .build()
        WorkManager.getInstance(application).enqueueUniqueWork(
            workName(modelId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Keep direct UIDT jobs outside WorkManager's declared 1000–1999 range. */
    private fun jobId(modelId: ModelId): Int = UIDT_JOB_ID_START +
        ((modelId.value.hashCode() and Int.MAX_VALUE) % UIDT_JOB_ID_RANGE)

    private fun workName(modelId: ModelId): String = "curated-model-${modelId.value}"
}

internal const val MODEL_ID_KEY = "model_id"
private const val UIDT_JOB_ID_START = 20_000
private const val UIDT_JOB_ID_RANGE = 1_000
