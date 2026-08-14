package com.dmitriim.localailab.source.models.diagnostics

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.StatFs
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.device.DeviceDiagnostics
import com.dmitriim.localailab.core.model.library.ModelCompatibility
import com.dmitriim.localailab.core.model.library.ModelCompatibilityState
import com.dmitriim.localailab.core.model.library.ModelValidationState
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.service.ModelDiagnostics
import com.dmitriim.localailab.source.models.library.InstalledModelService
import com.dmitriim.localailab.source.models.validation.ModelFileValidator
import com.dmitriim.localailab.source.models.validation.toReadableBytes
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelDiagnosticsService(
    private val application: Application,
    private val installedModels: InstalledModelService,
    private val validator: ModelFileValidator,
) : ModelDiagnostics {
    override suspend fun compatibility(model: ModelManifest): ModelCompatibility = withContext(Dispatchers.IO) {
        val reasons = mutableListOf<String>()
        if ("arm64-v8a" !in Build.SUPPORTED_ABIS) reasons += "${model.engineId.value} requires an arm64-v8a runtime."
        if (!validator.hasValidatorFor(model)) reasons += "The ${model.engineId.value} runtime is not packaged in this app build."
        val availableStorage = StatFs(application.filesDir.absolutePath).availableBytes
        val requiredStorage = model.files.sumOf { it.expectedBytes ?: 0L }
        if (requiredStorage > 0 && availableStorage < requiredStorage) {
            reasons += "At least ${requiredStorage.toReadableBytes()} of app storage is needed."
        }
        val availableRam = ActivityManager.MemoryInfo().also {
            application.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
        }.availMem
        model.approximateRamBytes?.takeIf { availableRam < it }?.let { recommendedRam ->
            reasons += "Available RAM is below this model's approximate ${recommendedRam.toReadableBytes()} recommendation."
        }
        when {
            reasons.any { it.contains("requires") || it.contains("not packaged") } ->
                ModelCompatibility(ModelCompatibilityState.INCOMPATIBLE, reasons)
            reasons.isNotEmpty() -> ModelCompatibility(ModelCompatibilityState.ADVISORY_WARNING, reasons)
            else -> ModelCompatibility(ModelCompatibilityState.COMPATIBLE, listOf("Compatible with the installed CPU runtime."))
        }
    }

    override suspend fun runDiagnostics(): DeviceDiagnostics = withContext(Dispatchers.IO) {
        val details = mutableListOf<String>()
        val rootDirectory = installedModels.rootDirectory
        val writable = rootDirectory.exists() || rootDirectory.mkdirs()
        val validCapabilities = mutableSetOf<AiCapability>()
        var filesValid = true
        rootDirectory.listFiles { file -> file.isDirectory }?.forEach { directory ->
            val manifest = installedModels.readInstalledManifest(directory)
            if (manifest == null) {
                filesValid = false
                details += "${directory.name}: manifest is missing or unreadable."
            } else {
                val result = validator.validate(manifest, directory)
                if (result.first == ModelValidationState.READY) {
                    validCapabilities += manifest.capabilities
                } else {
                    filesValid = false
                    details += "${manifest.displayName}: ${result.second}"
                }
            }
        }
        if (details.isEmpty()) details += "Installed manifests and model files are valid."
        DeviceDiagnostics(
            modelDirectoryWritable = writable,
            availableTemporaryBytes = StatFs(application.cacheDir.absolutePath).availableBytes,
            installedFilesValid = filesValid,
            offlineReadyCapabilities = validCapabilities,
            detail = details,
        )
    }
}
