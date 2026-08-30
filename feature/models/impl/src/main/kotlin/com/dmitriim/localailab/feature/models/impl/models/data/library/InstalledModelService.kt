package com.dmitriim.localailab.feature.models.impl.models.data.library

import android.app.Application
import android.util.Log
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.di.ApplicationCoroutineScope
import com.dmitriim.localailab.feature.models.api.data.ModelLibrary
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import com.dmitriim.localailab.feature.models.impl.models.data.persistence.InstalledModelEntity
import com.dmitriim.localailab.feature.models.impl.models.data.persistence.ModelsDatabaseProvider
import com.dmitriim.localailab.feature.models.impl.models.data.transfer.ModelTransferStateStore
import com.dmitriim.localailab.feature.models.impl.models.data.validation.FileSystemModelValidator
import com.dmitriim.localailab.feature.models.impl.models.data.validation.totalFileBytes
import com.dmitriim.localailab.feature.models.impl.models.domain.validation.ModelValidationResult
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class InstalledModelService(
    private val application: Application,
    private val databaseProvider: ModelsDatabaseProvider,
    private val validator: FileSystemModelValidator,
    private val transferState: ModelTransferStateStore,
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : ModelLibrary {
    private val dao = databaseProvider.database.installedModelDao()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    internal val rootDirectory = File(application.filesDir, "models")

    override val installedModels: Flow<List<InstalledModel>> = dao.observeAll().map { records ->
        records.mapNotNull { record ->
            val directory = File(rootDirectory, record.localDirectoryName)
            record.toDomainOrNull()?.takeIf { directory.isDirectory }
        }
    }

    init {
        rootDirectory.mkdirs()
        Log.i(TAG, "Model library initialized: rootDirectory=${rootDirectory.name}")
        applicationScope.launch(Dispatchers.IO) { reconcileInstalledModels() }
    }

    override suspend fun validate(modelId: ModelId): Result<InstalledModel> = runCatching {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Installed model validation started: modelId=${modelId.value}")
            val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
            val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
            val directory = File(rootDirectory, record.localDirectoryName)
            val validation = validator.validate(manifest, directory)
            val updated = record.copy(
                validationState = validation.state.name,
                validationMessage = validation.message,
                totalBytes = directory.totalFileBytes(),
            )
            dao.upsert(updated)
            Log.i(
                TAG,
                "Installed model validation completed: modelId=${modelId.value}, " +
                    "state=${validation.state}, bytes=${updated.totalBytes}, " +
                    "message=${validation.message}",
            )
            updated.toDomainOrNull() ?: error("The saved model manifest is invalid.")
        }
    }

    override suspend fun delete(modelId: ModelId): Result<Unit> = runCatching {
        val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
        val directory = File(rootDirectory, record.localDirectoryName)
        Log.i(
            TAG,
            "Installed model deletion started: modelId=${modelId.value}, " +
                "directory=${directory.name}, bytes=${directory.totalFileBytes()}",
        )
        require(directory.canonicalFile.parentFile == rootDirectory.canonicalFile) { "Invalid model directory." }
        if (directory.exists()) require(directory.deleteRecursively()) { "Could not delete model files." }
        dao.delete(modelId.value)
        transferState.delete(modelId)
        Log.i(TAG, "Installed model deletion completed: modelId=${modelId.value}")
    }

    internal suspend fun requireReadyModel(modelId: ModelId): InstalledModelLocation = withContext(Dispatchers.IO) {
        val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
        val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
        val directory = File(rootDirectory, record.localDirectoryName)
        val validation = validator.validate(manifest, directory)
        require(validation.state == ModelValidationState.READY) {
            validation.message ?: "The installed model is not ready."
        }
        InstalledModelLocation(manifest, directory)
    }

    internal suspend fun installDirectory(
        manifest: ModelManifest,
        temporary: File,
        verifyChecksums: Boolean = true,
    ) {
        Log.i(
            TAG,
            "Model installation validation started: modelId=${manifest.modelId.value}, " +
                "profile=${manifest.profileType}, verifyChecksums=$verifyChecksums",
        )
        val validation = validator.validate(manifest, temporary, verifyChecksums)
        require(validation.state == ModelValidationState.READY) { validation.message ?: "Model validation failed." }
        val enriched = validator.enrichChecksums(manifest, temporary)
        File(temporary, "manifest.json").writeText(json.encodeToString(enriched))
        val finalDirectory = File(rootDirectory, enriched.modelId.storageDirectoryName())
        withContext(NonCancellable) {
            require(!finalDirectory.exists()) { "A model with this ID is already installed." }
            require(temporary.renameTo(finalDirectory)) { "Could not complete the model installation transaction." }
            try {
                dao.upsert(enriched.toEntity(finalDirectory))
                Log.i(
                    TAG,
                    "Model installation completed: modelId=${enriched.modelId.value}, " +
                        "directory=${finalDirectory.name}, " +
                        "bytes=${finalDirectory.totalFileBytes()}, files=${enriched.files.size}",
                )
            } catch (error: Throwable) {
                Log.e(
                    TAG,
                    "Model installation database registration failed: " +
                        "modelId=${enriched.modelId.value}, message=${error.message}",
                    error,
                )
                finalDirectory.deleteRecursively()
                throw error
            }
        }
    }

    internal suspend fun registerInstalledDirectory(modelId: ModelId): Boolean = withContext(Dispatchers.IO) {
        dao.find(modelId.value)?.let { record ->
            if (File(rootDirectory, record.localDirectoryName).isDirectory) return@withContext true
            dao.delete(record.modelId)
        }
        val directory = File(rootDirectory, modelId.storageDirectoryName())
        val manifest = readInstalledManifest(directory) ?: return@withContext false
        if (manifest.modelId != modelId) return@withContext false
        val validation = validator.validate(manifest, directory)
        if (validation.state != ModelValidationState.READY) return@withContext false
        dao.upsert(manifest.toEntity(directory, validation))
        Log.i(TAG, "Existing model directory registered: modelId=${modelId.value}, directory=${directory.name}")
        true
    }

    internal fun readInstalledManifest(directory: File): ModelManifest? {
        if (!directory.isDirectory) return null
        return runCatching {
            json.decodeFromString<ModelManifest>(File(directory, "manifest.json").readText())
        }.getOrNull()
    }

    private suspend fun reconcileInstalledModels() {
        dao.all().forEach { record ->
            val directory = File(rootDirectory, record.localDirectoryName)
            if (!directory.isDirectory) {
                dao.delete(record.modelId)
                return@forEach
            }
            val manifest = runCatching { json.decodeFromString<ModelManifest>(record.manifestJson) }.getOrNull()
            val result = manifest?.let { validator.validate(it, directory) }
                ?: ModelValidationResult(ModelValidationState.INVALID, "The saved model manifest is unreadable.")
            if (record.validationState != result.state.name || record.validationMessage != result.message) {
                dao.upsert(record.copy(validationState = result.state.name, validationMessage = result.message))
            }
        }
        rootDirectory.listFiles { file -> file.isDirectory }?.forEach { directory ->
            val manifest = readInstalledManifest(directory) ?: return@forEach
            if (dao.find(manifest.modelId.value) == null) registerInstalledDirectory(manifest.modelId)
        }
    }

    private fun ModelManifest.toEntity(
        directory: File,
        validation: ModelValidationResult = ModelValidationResult(ModelValidationState.READY),
    ) = InstalledModelEntity(
        modelId = modelId.value,
        manifestJson = json.encodeToString(this),
        localDirectoryName = directory.name,
        totalBytes = directory.totalFileBytes(),
        validationState = validation.state.name,
        validationMessage = validation.message,
        lastUsedAtEpochMs = null,
    )

    private fun InstalledModelEntity.toDomainOrNull(): InstalledModel? = runCatching {
        InstalledModel(
            manifest = json.decodeFromString(manifestJson),
            localDirectoryName = localDirectoryName,
            totalBytes = totalBytes,
            validationState = ModelValidationState.valueOf(validationState),
            validationMessage = validationMessage,
            lastUsedAtEpochMs = lastUsedAtEpochMs,
        )
    }.getOrNull()

    private companion object {
        const val TAG = "AiP123Models"
    }
}

internal data class InstalledModelLocation(val manifest: ModelManifest, val directory: File)
