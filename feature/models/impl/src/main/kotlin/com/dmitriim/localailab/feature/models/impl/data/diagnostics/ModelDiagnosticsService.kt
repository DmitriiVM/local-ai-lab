package com.dmitriim.localailab.feature.models.impl.data.diagnostics

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.StatFs
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.device.DeviceDiagnostics
import com.dmitriim.localailab.core.model.engine.NativeAbiSupport
import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelCompatibility
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelDiagnostics
import com.dmitriim.localailab.feature.models.impl.data.library.InstalledModelService
import com.dmitriim.localailab.feature.models.impl.data.validation.FileSystemModelValidator
import com.dmitriim.localailab.feature.models.impl.data.validation.toReadableBytes
import com.dmitriim.localailab.feature.models.impl.domain.diagnostics.ModelCompatibilityIssue
import com.dmitriim.localailab.feature.models.impl.domain.diagnostics.ModelCompatibilityIssueSeverity
import com.dmitriim.localailab.feature.models.impl.domain.diagnostics.ModelCompatibilityPolicy
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
    private val validator: FileSystemModelValidator,
) : ModelDiagnostics {
    override suspend fun compatibility(model: ModelManifest): ModelCompatibility = withContext(Dispatchers.IO) {
        val issues = mutableListOf<ModelCompatibilityIssue>()
        if (!NativeAbiSupport.supports(Build.SUPPORTED_ABIS.toList())) {
            issues += ModelCompatibilityIssue(
                severity = ModelCompatibilityIssueSeverity.BLOCKING,
                message = "${model.engineId.value} requires an arm64-v8a or x86_64 runtime.",
            )
        }
        if (!validator.hasValidatorFor(model)) {
            issues += ModelCompatibilityIssue(
                severity = ModelCompatibilityIssueSeverity.BLOCKING,
                message = "The ${model.engineId.value} runtime is not packaged in this app build.",
            )
        }
        val availableStorage = StatFs(application.filesDir.absolutePath).availableBytes
        val requiredStorage = model.files.sumOf { it.expectedBytes ?: 0L }
        if (requiredStorage > 0 && availableStorage < requiredStorage) {
            issues += ModelCompatibilityIssue(
                severity = ModelCompatibilityIssueSeverity.ADVISORY,
                message = "At least ${requiredStorage.toReadableBytes()} of app storage is needed.",
            )
        }
        val availableRam = ActivityManager.MemoryInfo().also {
            application.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
        }.availMem
        model.approximateRamBytes?.takeIf { availableRam < it }?.let { recommendedRam ->
            issues += ModelCompatibilityIssue(
                severity = ModelCompatibilityIssueSeverity.ADVISORY,
                message = "Available RAM is below this model's approximate ${recommendedRam.toReadableBytes()} recommendation.",
            )
        }
        ModelCompatibilityPolicy.evaluate(issues)
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
                if (result.state == ModelValidationState.READY) {
                    validCapabilities += manifest.capabilities
                } else {
                    filesValid = false
                    details += "${manifest.displayName}: ${result.message}"
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
