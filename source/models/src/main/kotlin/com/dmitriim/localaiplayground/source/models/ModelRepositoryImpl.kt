package com.dmitriim.localaiplayground.source.models

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.StatFs
import android.provider.OpenableColumns
import com.dmitriim.localaiplayground.ai.api.ModelRuntimeLoader
import com.dmitriim.localaiplayground.ai.api.ModelRuntimeValidator
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.di.ApplicationCoroutineScope
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.ChatModelReference
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.DeviceDiagnostics
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelCompatibility
import com.dmitriim.localaiplayground.core.model.ModelCompatibilityState
import com.dmitriim.localaiplayground.core.model.ModelFileRole
import com.dmitriim.localaiplayground.core.model.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.ModelFormat
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelImportRequest
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelRepository
import com.dmitriim.localaiplayground.core.model.ModelSource
import com.dmitriim.localaiplayground.core.model.ModelTransferState
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import com.dmitriim.localaiplayground.core.model.SpeechToTextModelReference
import com.dmitriim.localaiplayground.source.database.InstalledModelEntity
import com.dmitriim.localaiplayground.source.database.ModelDatabaseProvider
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import kotlin.coroutines.coroutineContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelRepositoryImpl(
    private val application: Application,
    private val databaseProvider: ModelDatabaseProvider,
    private val validators: Set<ModelRuntimeValidator>,
    private val loaders: Set<ModelRuntimeLoader>,
    @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : ModelRepository {
    private val dao = databaseProvider.database.installedModelDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val rootDirectory = File(application.filesDir, "models")
    private val transferDirectory = File(application.cacheDir, "model-downloads")
    private val lifecycleMutex = Mutex()
    private val loadedHandles = linkedMapOf<ModelId, AutoCloseable>()
    private val mutableTransfers = MutableStateFlow<Map<ModelId, ModelTransferState>>(emptyMap())

    override val catalog: Flow<List<CatalogModel>> = MutableStateFlow(ModelCatalog.entries).asStateFlow()
    override val transfers: Flow<Map<ModelId, ModelTransferState>> = mutableTransfers.asStateFlow()
    override val installedModels: Flow<List<InstalledModel>> = dao.observeAll().map { records ->
        records.mapNotNull { record -> record.toDomainOrNull() }.map { installed ->
            installed.copy(loaded = synchronized(loadedHandles) { installed.manifest.modelId in loadedHandles })
        }
    }

    init {
        rootDirectory.mkdirs()
        transferDirectory.mkdirs()
        ModelDownloadRuntime.repository = this
        applicationScope.launch(Dispatchers.IO) { reconcileInstalledModels() }
    }

    override suspend fun import(request: ModelImportRequest): Result<ModelId> = runCatching {
        require(request.documentUris.isNotEmpty()) { "Select at least one model file." }
        withContext(Dispatchers.IO) {
            val modelId = ModelId("import-${UUID.randomUUID()}")
            val temporary = temporaryDirectory(modelId)
            try {
                val copiedNames = request.documentUris.map { uriString ->
                    val uri = android.net.Uri.parse(uriString)
                    val name = documentName(uri)
                    require(isSafeName(name)) { "The document name is not safe to install." }
                    require(copiedNamesSafe(temporary, name)) { "More than one selected document is named $name." }
                    application.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "The selected document is no longer readable." }
                        FileOutputStream(File(temporary, name)).use { output -> input.copyTo(output) }
                    }
                    name
                }
                val manifest = importedManifest(modelId, request, temporary, copiedNames)
                installDirectory(manifest, temporary)
                modelId
            } catch (error: Throwable) {
                temporary.deleteRecursively()
                throw error
            }
        }
    }

    override suspend fun download(modelId: ModelId): Result<Unit> = runCatching {
        val entry = ModelCatalog.entries.firstOrNull { it.manifest.modelId == modelId }
            ?: error("This catalog model is no longer available in the bundled catalog.")
        val compatibility = compatibility(entry.manifest)
        require(compatibility.state != ModelCompatibilityState.INCOMPATIBLE) {
            compatibility.reasons.joinToString()
        }
        ModelTransferScheduler(application).schedule(entry)
    }

    override suspend fun cancelTransfer(modelId: ModelId) {
        ModelTransferScheduler(application).cancel(modelId)
        mutableTransfers.update { it + (modelId to ModelTransferState.Cancelled) }
    }

    /** Called only by the API-specific JobService or WorkManager worker. */
    internal suspend fun executeScheduledDownload(modelId: ModelId): Result<Unit> = runCatching {
        val entry = ModelCatalog.entries.firstOrNull { it.manifest.modelId == modelId }
            ?: error("The requested catalog entry is not bundled with this app version.")
        withContext(Dispatchers.IO) { downloadAndInstall(entry) }
    }

    override suspend fun validate(modelId: ModelId): Result<InstalledModel> = runCatching {
        withContext(Dispatchers.IO) {
            val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
            val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
            val directory = File(rootDirectory, record.localDirectoryName)
            val validation = validateManifest(manifest, directory)
            val updated = record.copy(
                validationState = validation.first.name,
                validationMessage = validation.second,
                totalBytes = directory.walkTopDown().filter { it.isFile }.sumOf { it.length() },
            )
            dao.upsert(updated)
            updated.toDomainOrNull() ?: error("The saved model manifest is invalid.")
        }
    }

    override suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference> = runCatching {
        withContext(Dispatchers.IO) {
            val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
            val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
            require(AiCapability.CHAT in manifest.capabilities && manifest.profileType == RuntimeProfileType.LLM) {
                "This installed model is not a compatible local chat model."
            }
            val directory = File(rootDirectory, record.localDirectoryName)
            val validation = validateManifest(manifest, directory)
            require(validation.first == ModelValidationState.READY) {
                validation.second ?: "The installed chat model is not ready."
            }
            val primary = requireNotNull(manifest.files.firstOrNull { it.role == ModelFileRole.PRIMARY_MODEL }) {
                "The chat model does not declare a primary GGUF file."
            }
            ChatModelReference(
                modelId = modelId,
                displayName = manifest.displayName,
                modelPath = File(directory, primary.relativePath).absolutePath,
                defaultContextSize = manifest.contextSize ?: 512,
            )
        }
    }

    override suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference> = runCatching {
        withContext(Dispatchers.IO) {
            val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
            val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
            require(AiCapability.SPEECH_TO_TEXT in manifest.capabilities && manifest.profileType == RuntimeProfileType.WHISPER_STT) {
                "This installed model is not a compatible Whisper speech-to-text model."
            }
            val directory = File(rootDirectory, record.localDirectoryName)
            val validation = validateManifest(manifest, directory)
            require(validation.first == ModelValidationState.READY) {
                validation.second ?: "The installed speech model is not ready."
            }
            SpeechToTextModelReference(
                modelId = modelId,
                displayName = manifest.displayName,
                modelDirectory = directory.absolutePath,
                sampleRateHz = manifest.sampleRateHz ?: 16_000,
                languages = manifest.languages,
            )
        }
    }

    override suspend fun compatibility(model: ModelManifest): ModelCompatibility = withContext(Dispatchers.IO) {
        val reasons = mutableListOf<String>()
        if ("arm64-v8a" !in Build.SUPPORTED_ABIS) {
            reasons += "${model.engineId.value} requires an arm64-v8a runtime."
        }
        if (validators.none { it.engineId == model.engineId }) {
            reasons += "The ${model.engineId.value} runtime is not packaged in this app build."
        }
        val availableStorage = StatFs(application.filesDir.absolutePath).availableBytes
        val requiredStorage = model.files.sumOf { it.expectedBytes ?: 0L }
        if (requiredStorage > 0 && availableStorage < requiredStorage) {
            reasons += "At least ${requiredStorage.toReadableBytes()} of app storage is needed."
        }
        val availableRam = ActivityManager.MemoryInfo().also {
            application.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
        }.availMem
        val recommendedRam = model.approximateRamBytes
        if (recommendedRam != null && availableRam < recommendedRam) {
            reasons += "Available RAM is below this model's approximate ${recommendedRam.toReadableBytes()} recommendation."
        }
        when {
            reasons.any { it.contains("requires") || it.contains("not packaged") } ->
                ModelCompatibility(ModelCompatibilityState.INCOMPATIBLE, reasons)
            reasons.isNotEmpty() -> ModelCompatibility(ModelCompatibilityState.ADVISORY_WARNING, reasons)
            else -> ModelCompatibility(ModelCompatibilityState.COMPATIBLE, listOf("Compatible with the installed CPU runtime."))
        }
    }

    override suspend fun load(modelId: ModelId): Result<Unit> = runCatching {
        lifecycleMutex.withLock {
            if (loadedHandles.containsKey(modelId)) return@withLock
            val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
            val manifest = json.decodeFromString<ModelManifest>(record.manifestJson)
            val directory = File(rootDirectory, record.localDirectoryName)
            val check = validateManifest(manifest, directory)
            require(check.first == ModelValidationState.READY) { check.second ?: "Model validation failed." }
            val compatibility = compatibility(manifest)
            require(compatibility.state != ModelCompatibilityState.INCOMPATIBLE) { compatibility.reasons.joinToString() }
            val loader = loaders.firstOrNull { it.engineId == manifest.engineId }
                ?: error("No loader is available for ${manifest.engineId.value}.")
            val handle = withContext(Dispatchers.IO) { loader.load(manifest, directory) }
            loadedHandles[modelId] = handle
            dao.upsert(record.copy(lastUsedAtEpochMs = System.currentTimeMillis()))
        }
    }

    override suspend fun unload(modelId: ModelId): Result<Unit> = runCatching {
        lifecycleMutex.withLock {
            loadedHandles.remove(modelId)?.close()
        }
    }

    override suspend fun delete(modelId: ModelId): Result<Unit> = runCatching {
        lifecycleMutex.withLock {
            val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
            loadedHandles.remove(modelId)?.close()
            val directory = File(rootDirectory, record.localDirectoryName)
            require(directory.canonicalFile.parentFile == rootDirectory.canonicalFile) { "Invalid model directory." }
            if (directory.exists()) require(directory.deleteRecursively()) { "Could not delete model files." }
            dao.delete(modelId.value)
        }
    }

    override suspend fun runDiagnostics(): DeviceDiagnostics = withContext(Dispatchers.IO) {
        val details = mutableListOf<String>()
        val writable = rootDirectory.exists() || rootDirectory.mkdirs()
        val validCapabilities = mutableSetOf<AiCapability>()
        var filesValid = true
        // Room's Flow is deliberately not collected here. Query each installed directory from its DAO-backed IDs on startup flows.
        // Diagnostics instead validates the manifests currently represented by the local directory scan.
        rootDirectory.listFiles { file -> file.isDirectory }?.forEach { directory ->
            val manifestFile = File(directory, "manifest.json")
            val manifest = runCatching { json.decodeFromString<ModelManifest>(manifestFile.readText()) }.getOrNull()
            if (manifest == null) {
                filesValid = false
                details += "${directory.name}: manifest is missing or unreadable."
            } else {
                val result = validateManifest(manifest, directory)
                if (result.first == ModelValidationState.READY) validCapabilities += manifest.capabilities
                else {
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

    private suspend fun downloadAndInstall(entry: CatalogModel) {
        val modelId = entry.manifest.modelId
        val temporary = temporaryDirectory(modelId)
        mutableTransfers.update { it + (modelId to ModelTransferState.Running(0, entry.download.expectedBytes)) }
        try {
            val payload = File(temporary, "payload")
            downloadTo(entry, payload)
            require(payload.length() == entry.download.expectedBytes) { "Downloaded size does not match the catalog." }
            require(payload.sha256().equals(entry.download.sha256, ignoreCase = true)) { "Downloaded checksum does not match the catalog." }
            if (entry.download.archive) extractArchivePayload(payload, temporary, entry.manifest)
            else {
                val destination = File(temporary, entry.manifest.files.single().relativePath)
                require(payload.renameTo(destination)) { "Could not prepare the downloaded model file." }
            }
            installDirectory(entry.manifest.copy(installedAtEpochMs = System.currentTimeMillis()), temporary)
            mutableTransfers.update { it + (modelId to ModelTransferState.Completed) }
        } catch (cancelled: CancellationException) {
            temporary.deleteRecursively()
            mutableTransfers.update { it + (modelId to ModelTransferState.Cancelled) }
            throw cancelled
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            mutableTransfers.update { it + (modelId to ModelTransferState.Failed(error.message ?: "Download failed.")) }
            throw error
        }
    }

    private suspend fun downloadTo(entry: CatalogModel, destination: File) {
        var url = entry.download.url
        repeat(5) {
            coroutineContext.ensureActive()
            val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    url = requireNotNull(connection.getHeaderField("Location")) { "Redirect without a target." }
                    return@repeat
                }
                require(status in 200..299) { "Download failed with HTTP $status." }
                connection.getHeaderFieldLong("Content-Length", -1).takeIf { it >= 0 }?.let { announced ->
                    require(announced == entry.download.expectedBytes) { "Server response length does not match the catalog." }
                }
                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var completed = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            completed += read
                            mutableTransfers.update { states -> states + (entry.manifest.modelId to ModelTransferState.Running(completed, entry.download.expectedBytes)) }
                        }
                    }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        error("The download redirected too many times.")
    }

    private fun extractArchivePayload(payload: File, temporary: File, manifest: ModelManifest) {
        val extracted = File(temporary, "extracted").also { it.mkdirs() }
        FileInputStream(payload).use { fileInput ->
            BZip2CompressorInputStream(fileInput).use { bzip ->
                TarArchiveInputStream(bzip).use { tar ->
                    while (true) {
                        val entry = tar.nextTarEntry ?: break
                        if (entry.isDirectory) continue
                        val out = File(extracted, entry.name)
                        require(out.canonicalPath.startsWith(extracted.canonicalPath + File.separator)) { "Archive contains an unsafe path." }
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { tar.copyTo(it) }
                    }
                }
            }
        }
        manifest.files.filter { it.required }.forEach { spec ->
            val source = extracted.walkTopDown().firstOrNull { it.isFile && it.name == File(spec.relativePath).name }
                ?: error("Archive does not contain ${spec.relativePath}.")
            val destination = File(temporary, spec.relativePath)
            destination.parentFile?.mkdirs()
            source.copyTo(destination, overwrite = true)
        }
        extracted.deleteRecursively()
        payload.delete()
    }

    private suspend fun installDirectory(manifest: ModelManifest, temporary: File) {
        val enriched = enrichFileChecksums(manifest, temporary)
        val validation = validateManifest(enriched, temporary)
        require(validation.first == ModelValidationState.READY) { validation.second ?: "Model validation failed." }
        File(temporary, "manifest.json").writeText(json.encodeToString(enriched))
        val finalDirectory = File(rootDirectory, directoryName(enriched.modelId))
        require(!finalDirectory.exists()) { "A model with this ID is already installed." }
        require(temporary.renameTo(finalDirectory)) { "Could not complete the model installation transaction." }
        try {
            dao.upsert(
                InstalledModelEntity(
                    modelId = enriched.modelId.value,
                    manifestJson = json.encodeToString(enriched),
                    localDirectoryName = finalDirectory.name,
                    totalBytes = finalDirectory.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                    validationState = ModelValidationState.READY.name,
                    validationMessage = null,
                    lastUsedAtEpochMs = null,
                ),
            )
        } catch (error: Throwable) {
            finalDirectory.deleteRecursively()
            throw error
        }
    }

    private fun importedManifest(
        modelId: ModelId,
        request: ModelImportRequest,
        directory: File,
        copiedNames: List<String>,
    ): ModelManifest {
        val files = roleSpecsForImport(request.profileType, copiedNames)
        return ModelManifest(
            modelId = modelId,
            displayName = request.displayName.ifBlank { "Imported model" },
            family = "Imported",
            capabilities = capabilitiesFor(request.profileType),
            engineId = request.engineId,
            profileType = request.profileType,
            format = if (request.profileType == RuntimeProfileType.LLM) ModelFormat.GGUF else ModelFormat.ONNX,
            files = files,
            source = ModelSource(null, licenseName = "User supplied", attribution = "Imported from a user-selected document."),
            installedAtEpochMs = System.currentTimeMillis(),
        )
    }

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

    private fun validateManifest(manifest: ModelManifest, directory: File): Pair<ModelValidationState, String?> {
        if (!directory.isDirectory) return ModelValidationState.MISSING_FILES to "The installed model directory is missing."
        manifest.files.filter { it.required }.forEach { spec ->
            val file = File(directory, spec.relativePath)
            if (!file.isFile || !file.canRead()) return ModelValidationState.MISSING_FILES to "Missing ${spec.relativePath}."
            if (spec.expectedBytes != null && file.length() != spec.expectedBytes) {
                return ModelValidationState.INVALID to "${spec.relativePath} has an unexpected size."
            }
            if (spec.sha256 != null && !file.sha256().equals(spec.sha256, ignoreCase = true)) {
                return ModelValidationState.INVALID to "${spec.relativePath} has an unexpected checksum."
            }
        }
        val validator = validators.firstOrNull { it.engineId == manifest.engineId }
            ?: return ModelValidationState.INCOMPATIBLE to "No ${manifest.engineId.value} validator is packaged."
        val result = validator.validate(manifest, directory)
        return if (result.valid) ModelValidationState.READY to null
        else ModelValidationState.INVALID to (result.message ?: "Engine metadata validation failed.")
    }

    private fun enrichFileChecksums(manifest: ModelManifest, directory: File): ModelManifest = manifest.copy(
        files = manifest.files.map { spec ->
            val file = File(directory, spec.relativePath)
            if (file.isFile) spec.copy(expectedBytes = file.length(), sha256 = file.sha256()) else spec
        },
    )

    private fun documentName(uri: android.net.Uri): String = application.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment?.substringAfterLast('/') ?: error("The selected document has no name.")

    private fun temporaryDirectory(modelId: ModelId): File = File(
        rootDirectory.parentFile,
        "model-installing-${modelId.value}-${UUID.randomUUID()}",
    ).also { require(it.mkdirs()) { "Could not create the installation staging directory." } }

    private fun directoryName(id: ModelId): String = id.value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun isSafeName(name: String): Boolean = name.isNotBlank() && !name.contains('/') && !name.contains('\\') && name != "." && name != ".."
    private fun copiedNamesSafe(directory: File, name: String): Boolean = !File(directory, name).exists()
    private fun capabilitiesFor(profile: RuntimeProfileType): Set<AiCapability> = when (profile) {
        RuntimeProfileType.LLM -> setOf(AiCapability.CHAT)
        RuntimeProfileType.WHISPER_STT -> setOf(AiCapability.SPEECH_TO_TEXT)
        RuntimeProfileType.SILERO_VAD -> setOf(AiCapability.SPEECH_TO_TEXT, AiCapability.VOICE_ASSISTANT)
        RuntimeProfileType.SUPERTONIC_TTS -> setOf(AiCapability.TEXT_TO_SPEECH, AiCapability.VOICE_ASSISTANT)
    }

    /** Rebuilds reliable state after process restart without allocating any native model. */
    private suspend fun reconcileInstalledModels() {
        dao.all().forEach { record ->
            val manifest = runCatching { json.decodeFromString<ModelManifest>(record.manifestJson) }.getOrNull()
            val result = manifest?.let { validateManifest(it, File(rootDirectory, record.localDirectoryName)) }
                ?: (ModelValidationState.INVALID to "The saved model manifest is unreadable.")
            if (record.validationState != result.first.name || record.validationMessage != result.second) {
                dao.upsert(record.copy(validationState = result.first.name, validationMessage = result.second))
            }
        }
    }

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
}

private fun File.sha256(): String = FileInputStream(this).use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

private fun Long.toReadableBytes(): String = "%.1f GiB".format(toDouble() / 1024 / 1024 / 1024)
