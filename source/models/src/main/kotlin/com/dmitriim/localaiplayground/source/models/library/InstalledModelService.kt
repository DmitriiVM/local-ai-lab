package com.dmitriim.localaiplayground.source.models.library

import android.app.Application
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.di.ApplicationCoroutineScope
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelFileRole
import com.dmitriim.localaiplayground.core.model.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.ModelFormat
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelImportRequest
import com.dmitriim.localaiplayground.core.model.ModelLibrary
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelSource
import com.dmitriim.localaiplayground.core.model.ModelTransferState
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import com.dmitriim.localaiplayground.source.database.InstalledModelEntity
import com.dmitriim.localaiplayground.source.database.ModelDatabaseProvider
import com.dmitriim.localaiplayground.source.models.transfer.ModelTransferStateStore
import com.dmitriim.localaiplayground.source.models.validation.ModelFileValidator
import com.dmitriim.localaiplayground.source.models.validation.totalFileBytes
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
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
    private val databaseProvider: ModelDatabaseProvider,
    private val validator: ModelFileValidator,
    private val transferState: ModelTransferStateStore,
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : ModelLibrary {
    private val dao = databaseProvider.database.installedModelDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    internal val rootDirectory = File(application.filesDir, "models")

    override val installedModels: Flow<List<InstalledModel>> = dao.observeAll().map { records ->
        records.mapNotNull { record ->
            val directory = File(rootDirectory, record.localDirectoryName)
            record.toDomainOrNull()?.takeIf { directory.isDirectory }
        }
    }

    init {
        rootDirectory.mkdirs()
        cleanupInterruptedInstallations()
        applicationScope.launch(Dispatchers.IO) { reconcileInstalledModels() }
    }

    override suspend fun import(request: ModelImportRequest): Result<ModelId> = runCatching {
        require(request.documentUris.isNotEmpty()) { "Select at least one model file." }
        withContext(Dispatchers.IO) {
            val modelId = ModelId("import-${UUID.randomUUID()}")
            val temporary = temporaryDirectory(modelId)
            try {
                val copiedNames = request.documentUris.map { uriString ->
                    val uri = uriString.toUri()
                    val name = documentName(uri)
                    require(isSafeName(name)) { "The document name is not safe to install." }
                    require(copiedNamesSafe(temporary, name)) { "More than one selected document is named $name." }
                    application.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "The selected document is no longer readable." }
                        FileOutputStream(File(temporary, name)).use { output -> input.copyTo(output) }
                    }
                    name
                }
                installDirectory(importedManifest(modelId, request, copiedNames), temporary)
                modelId
            } catch (error: Throwable) {
                temporary.deleteRecursively()
                throw error
            }
        }
    }

    override suspend fun validate(modelId: ModelId): Result<InstalledModel> = runCatching {
        withContext(Dispatchers.IO) {
            val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
            val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
            val directory = File(rootDirectory, record.localDirectoryName)
            val validation = validator.validate(manifest, directory)
            val updated = record.copy(
                validationState = validation.first.name,
                validationMessage = validation.second,
                totalBytes = directory.totalFileBytes(),
            )
            dao.upsert(updated)
            updated.toDomainOrNull() ?: error("The saved model manifest is invalid.")
        }
    }

    override suspend fun delete(modelId: ModelId): Result<Unit> = runCatching {
        val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
        val directory = File(rootDirectory, record.localDirectoryName)
        require(directory.canonicalFile.parentFile == rootDirectory.canonicalFile) { "Invalid model directory." }
        if (directory.exists()) require(directory.deleteRecursively()) { "Could not delete model files." }
        dao.delete(modelId.value)
        transferState.update { it + (modelId to ModelTransferState.Idle) }
    }

    internal suspend fun requireReadyModel(modelId: ModelId): InstalledModelLocation = withContext(Dispatchers.IO) {
        val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
        val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
        val directory = File(rootDirectory, record.localDirectoryName)
        val validation = validator.validate(manifest, directory)
        require(validation.first == ModelValidationState.READY) {
            validation.second ?: "The installed model is not ready."
        }
        InstalledModelLocation(manifest, directory)
    }

    internal suspend fun installDirectory(
        manifest: ModelManifest,
        temporary: File,
        verifyChecksums: Boolean = true,
    ) {
        val validation = validator.validate(manifest, temporary, verifyChecksums)
        require(validation.first == ModelValidationState.READY) { validation.second ?: "Model validation failed." }
        val enriched = validator.enrichChecksums(manifest, temporary)
        File(temporary, "manifest.json").writeText(json.encodeToString(enriched))
        val finalDirectory = File(rootDirectory, directoryName(enriched.modelId))
        withContext(NonCancellable) {
            require(!finalDirectory.exists()) { "A model with this ID is already installed." }
            require(temporary.renameTo(finalDirectory)) { "Could not complete the model installation transaction." }
            try {
                dao.upsert(enriched.toEntity(finalDirectory))
            } catch (error: Throwable) {
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
        val directory = File(rootDirectory, directoryName(modelId))
        val manifest = readInstalledManifest(directory) ?: return@withContext false
        if (manifest.modelId != modelId) return@withContext false
        val validation = validator.validate(manifest, directory)
        if (validation.first != ModelValidationState.READY) return@withContext false
        dao.upsert(manifest.toEntity(directory, validation))
        true
    }

    internal fun readInstalledManifest(directory: File): ModelManifest? {
        if (!directory.isDirectory) return null
        return runCatching {
            json.decodeFromString<ModelManifest>(File(directory, "manifest.json").readText())
        }.getOrNull()
    }

    private fun importedManifest(modelId: ModelId, request: ModelImportRequest, copiedNames: List<String>) = ModelManifest(
        modelId = modelId,
        displayName = request.displayName.ifBlank { "Imported model" },
        family = "Imported",
        capabilities = capabilitiesFor(request.profileType),
        engineId = request.engineId,
        profileType = request.profileType,
        format = if (request.profileType == RuntimeProfileType.LLM) ModelFormat.GGUF else ModelFormat.ONNX,
        files = roleSpecsForImport(request.profileType, copiedNames),
        source = ModelSource(null, licenseName = "User supplied", attribution = "Imported from a user-selected document."),
        installedAtEpochMs = System.currentTimeMillis(),
    )

    private fun roleSpecsForImport(profile: RuntimeProfileType, names: List<String>): List<ModelFileSpec> {
        fun name(required: String): String = names.firstOrNull { it == required }
            ?: error("Missing $required. Select all required companion files.")
        return when (profile) {
            RuntimeProfileType.LLM -> listOf(ModelFileSpec(names.singleOrNull { it.endsWith(".gguf", true) }
                ?: error("Select exactly one .gguf file."), ModelFileRole.PRIMARY_MODEL))
            RuntimeProfileType.WHISPER_STT -> listOf(
                ModelFileSpec(name("base-encoder.int8.onnx"), ModelFileRole.ENCODER),
                ModelFileSpec(name("base-decoder.int8.onnx"), ModelFileRole.DECODER),
                ModelFileSpec(name("base-tokens.txt"), ModelFileRole.TOKENS),
            )
            RuntimeProfileType.SILERO_VAD -> listOf(ModelFileSpec(name("silero_vad.onnx"), ModelFileRole.VAD_MODEL))
            RuntimeProfileType.SUPERTONIC_TTS -> listOf(
                ModelFileSpec(name("duration_predictor.int8.onnx"), ModelFileRole.DURATION_PREDICTOR),
                ModelFileSpec(name("text_encoder.int8.onnx"), ModelFileRole.TEXT_ENCODER),
                ModelFileSpec(name("vector_estimator.int8.onnx"), ModelFileRole.VECTOR_ESTIMATOR),
                ModelFileSpec(name("vocoder.int8.onnx"), ModelFileRole.VOCODER),
                ModelFileSpec(name("tts.json"), ModelFileRole.CONFIG),
                ModelFileSpec(name("unicode_indexer.bin"), ModelFileRole.UNICODE_INDEXER),
                ModelFileSpec(name("voice.bin"), ModelFileRole.VOICE_STYLE),
            )
        }
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
                ?: (ModelValidationState.INVALID to "The saved model manifest is unreadable.")
            if (record.validationState != result.first.name || record.validationMessage != result.second) {
                dao.upsert(record.copy(validationState = result.first.name, validationMessage = result.second))
            }
        }
        rootDirectory.listFiles { file -> file.isDirectory }?.forEach { directory ->
            val manifest = readInstalledManifest(directory) ?: return@forEach
            if (dao.find(manifest.modelId.value) == null) registerInstalledDirectory(manifest.modelId)
        }
    }

    private fun temporaryDirectory(modelId: ModelId): File = File(
        rootDirectory.parentFile,
        "model-installing-${modelId.value}-${UUID.randomUUID()}",
    ).also { require(it.mkdirs()) { "Could not create the installation staging directory." } }

    private fun cleanupInterruptedInstallations() {
        rootDirectory.parentFile
            ?.listFiles { file -> file.isDirectory && file.name.startsWith("model-installing-") }
            ?.forEach(File::deleteRecursively)
    }

    private fun documentName(uri: android.net.Uri): String = application.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment?.substringAfterLast('/') ?: error("The selected document has no name.")

    private fun ModelManifest.toEntity(
        directory: File,
        validation: Pair<ModelValidationState, String?> = ModelValidationState.READY to null,
    ) = InstalledModelEntity(
        modelId = modelId.value,
        manifestJson = json.encodeToString(this),
        localDirectoryName = directory.name,
        totalBytes = directory.totalFileBytes(),
        validationState = validation.first.name,
        validationMessage = validation.second,
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

    private fun directoryName(id: ModelId): String = id.value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun isSafeName(name: String) = name.isNotBlank() && !name.contains('/') && !name.contains('\\') && name != "." && name != ".."
    private fun copiedNamesSafe(directory: File, name: String) = !File(directory, name).exists()
    private fun capabilitiesFor(profile: RuntimeProfileType): Set<AiCapability> = when (profile) {
        RuntimeProfileType.LLM -> setOf(AiCapability.CHAT)
        RuntimeProfileType.WHISPER_STT -> setOf(AiCapability.SPEECH_TO_TEXT)
        RuntimeProfileType.SILERO_VAD -> setOf(AiCapability.SPEECH_TO_TEXT, AiCapability.VOICE_ASSISTANT)
        RuntimeProfileType.SUPERTONIC_TTS -> setOf(AiCapability.TEXT_TO_SPEECH, AiCapability.VOICE_ASSISTANT)
    }
}

internal data class InstalledModelLocation(val manifest: ModelManifest, val directory: File)
