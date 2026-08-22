package com.dmitriim.localailab.source.models.transfer

import android.annotation.TargetApi
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.PersistableBundle
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelTransferNetworkPolicy
import com.dmitriim.localailab.core.model.manifest.ModelId
import java.util.concurrent.TimeUnit

/** Selects one persisted scheduling mechanism; both invoke the same transactional installer. */
internal class ModelTransferScheduler(
    private val application: Application,
) {
    fun schedule(
        entry: CatalogModel,
        executionGeneration: Long,
        networkPolicy: ModelTransferNetworkPolicy,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            scheduleUserInitiatedTransfer(entry, executionGeneration, networkPolicy)
        } else {
            scheduleForegroundWorker(entry.manifest.modelId, executionGeneration, networkPolicy)
        }
    }

    fun cancel(modelId: ModelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            application.getSystemService(JobScheduler::class.java).cancel(jobId(modelId))
        }
        WorkManager.getInstance(application).cancelUniqueWork(workName(modelId))
    }

    fun scheduleRetry(
        modelId: ModelId,
        executionGeneration: Long,
        networkPolicy: ModelTransferNetworkPolicy,
        delayMillis: Long,
    ) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(workConstraints(networkPolicy))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    MODEL_ID_KEY to modelId.value,
                    MODEL_TRANSFER_GENERATION_KEY to executionGeneration,
                ),
            )
            .build()
        WorkManager.getInstance(application).enqueueUniqueWork(
            workName(modelId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun scheduleUserInitiatedTransfer(
        entry: CatalogModel,
        executionGeneration: Long,
        networkPolicy: ModelTransferNetworkPolicy,
    ) {
        val extras = PersistableBundle().apply {
            putString(MODEL_ID_KEY, entry.manifest.modelId.value)
            putLong(MODEL_TRANSFER_GENERATION_KEY, executionGeneration)
        }
        val job = JobInfo.Builder(jobId(entry.manifest.modelId), ComponentName(application, ModelDownloadJobService::class.java))
            .setRequiredNetwork(networkRequest(networkPolicy))
            .setRequiresBatteryNotLow(true)
            .setEstimatedNetworkBytes(entry.download.expectedBytes, 0)
            .setUserInitiated(true)
            .setExtras(extras)
            .build()
        check(application.getSystemService(JobScheduler::class.java).schedule(job) == JobScheduler.RESULT_SUCCESS) {
            "Android could not schedule this user-initiated download."
        }
    }

    private fun scheduleForegroundWorker(
        modelId: ModelId,
        executionGeneration: Long,
        networkPolicy: ModelTransferNetworkPolicy,
    ) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(workConstraints(networkPolicy))
            .setInputData(
                workDataOf(
                    MODEL_ID_KEY to modelId.value,
                    MODEL_TRANSFER_GENERATION_KEY to executionGeneration,
                ),
            )
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

    private fun networkRequest(policy: ModelTransferNetworkPolicy): NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)
            }
            if (policy == ModelTransferNetworkPolicy.WIFI_ONLY) {
                addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            }
        }
        .build()

    private fun workConstraints(policy: ModelTransferNetworkPolicy): Constraints = Constraints.Builder().apply {
        setRequiresBatteryNotLow(true)
        val fallbackType = when (policy) {
            ModelTransferNetworkPolicy.WIFI_ONLY -> NetworkType.UNMETERED
            ModelTransferNetworkPolicy.ANY_NETWORK -> NetworkType.CONNECTED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setRequiredNetworkRequest(networkRequest(policy), fallbackType)
        } else {
            setRequiredNetworkType(fallbackType)
        }
    }.build()
}

internal const val MODEL_ID_KEY = "model_id"
internal const val MODEL_TRANSFER_GENERATION_KEY = "model_transfer_generation"
private const val UIDT_JOB_ID_START = 20_000
private const val UIDT_JOB_ID_RANGE = 1_000
