package com.dmitriim.localailab.source.models.library

import android.app.Application
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import com.dmitriim.localailab.ai.api.model.ModelImportDefinition
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.di.ApplicationCoroutineScope
import com.dmitriim.localailab.core.model.library.InstalledModel
import com.dmitriim.localailab.core.model.library.ModelImportRequest
import com.dmitriim.localailab.core.model.library.ModelValidationState
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelSource
import com.dmitriim.localailab.core.model.service.ModelLibrary
import com.dmitriim.localailab.source.database.InstalledModelEntity
import com.dmitriim.localailab.source.database.ModelDatabaseProvider
import com.dmitriim.localailab.source.models.transfer.ModelTransferStateStore
import com.dmitriim.localailab.source.models.validation.ModelFileValidator
import com.dmitriim.localailab.source.models.validation.totalFileBytes
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
    private val profiles: ModelRuntimeProfileRegistry,
    private val validator: ModelFileValidator,
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
        cleanupInterruptedInstallations()
        Log.i(TAG, "Model library initialized: rootDirectory=${rootDirectory.name}")
        applicationScope.launch(Dispatchers.IO) { reconcileInstalledModels() }
    }

    override suspend fun import(request: ModelImportRequest): Result<ModelId> = runCatching {
        require(request.documentUris.isNotEmpty() || request.directoryUri != null) { "Select model files or an extracted model directory." }
        val profile = profiles.requireRuntimeProfile(request.profileKey)
        val importDefinition = requireNotNull(profile.importDefinition) {
            "${profile.displayName} does not support importing ${request.profileType.value}."
        }
        Log.i(TAG, "Model import started: profile=${request.profileType}, engine=${request.engineId.value}, documentCount=${request.documentUris.size}")
        withContext(Dispatchers.IO) {
            val modelId = ModelId("import-${UUID.randomUUID()}")
            val temporary = temporaryDirectory(modelId)
            try {
                val copiedNames = request.documentUris.map { uriString ->
                    val uri = uriString.toUri()
                    val name = documentName(uri)
                    require(ModelImportPolicy.isSafeFileName(name)) { "The document name is not safe to install." }
                    require(copiedNamesSafe(temporary, name)) { "More than one selected document is named $name." }
                    application.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "The selected document is no longer readable." }
                        val destination = ModelImportPolicy.destination(temporary, name)
                        FileOutputStream(destination).use { output -> input.copyTo(output) }
                        Log.i(TAG, "Model import file copied: name=$name, bytes=${destination.length()}")
                    }
                    name
                } + request.directoryUri?.let { treeUri -> copyDirectoryTree(treeUri.toUri(), temporary) }.orEmpty()
                installDirectory(importedManifest(modelId, request, profile, importDefinition, copiedNames), temporary)
                Log.i(TAG, "Model import completed: modelId=${modelId.value}, files=${copiedNames.size}")
                modelId
            } catch (error: Throwable) {
                Log.e(TAG, "Model import failed: modelId=${modelId.value}, message=${error.message}", error)
                temporary.deleteRecursively()
                throw error
            }
        }
    }

    override suspend fun validate(modelId: ModelId): Result<InstalledModel> = runCatching {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Installed model validation started: modelId=${modelId.value}")
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
            Log.i(TAG, "Installed model validation completed: modelId=${modelId.value}, state=${validation.first}, bytes=${updated.totalBytes}, message=${validation.second}")
            updated.toDomainOrNull() ?: error("The saved model manifest is invalid.")
        }
    }

    override suspend fun delete(modelId: ModelId): Result<Unit> = runCatching {
        val record = requireNotNull(dao.find(modelId.value)) { "This model is not installed." }
        val directory = File(rootDirectory, record.localDirectoryName)
        Log.i(TAG, "Installed model deletion started: modelId=${modelId.value}, directory=${directory.name}, bytes=${directory.totalFileBytes()}")
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
        Log.i(TAG, "Model installation validation started: modelId=${manifest.modelId.value}, profile=${manifest.profileType}, verifyChecksums=$verifyChecksums")
        val validation = validator.validate(manifest, temporary, verifyChecksums)
        require(validation.first == ModelValidationState.READY) { validation.second ?: "Model validation failed." }
        val enriched = validator.enrichChecksums(manifest, temporary)
        File(temporary, "manifest.json").writeText(json.encodeToString(enriched))
        val finalDirectory = File(rootDirectory, ModelImportPolicy.directoryName(enriched.modelId))
        withContext(NonCancellable) {
            require(!finalDirectory.exists()) { "A model with this ID is already installed." }
            require(temporary.renameTo(finalDirectory)) { "Could not complete the model installation transaction." }
            try {
                dao.upsert(enriched.toEntity(finalDirectory))
                Log.i(TAG, "Model installation completed: modelId=${enriched.modelId.value}, directory=${finalDirectory.name}, bytes=${finalDirectory.totalFileBytes()}, files=${enriched.files.size}")
            } catch (error: Throwable) {
                Log.e(TAG, "Model installation database registration failed: modelId=${enriched.modelId.value}, message=${error.message}", error)
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
        val directory = File(rootDirectory, ModelImportPolicy.directoryName(modelId))
        val manifest = readInstalledManifest(directory) ?: return@withContext false
        if (manifest.modelId != modelId) return@withContext false
        val validation = validator.validate(manifest, directory)
        if (validation.first != ModelValidationState.READY) return@withContext false
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

    private fun importedManifest(
        modelId: ModelId,
        request: ModelImportRequest,
        profile: ModelRuntimeProfile,
        definition: ModelImportDefinition,
        copiedNames: List<String>,
    ) = ModelManifest(
        modelId = modelId,
        displayName = request.displayName.ifBlank { definition.displayName },
        family = "Imported",
        capabilities = profile.capabilities,
        engineId = request.engineId,
        profileType = request.profileType,
        format = definition.format,
        files = ModelImportPolicy.roleSpecs(definition, copiedNames),
        source = ModelSource(null, licenseName = "User supplied", attribution = "Imported from a user-selected document."),
        installedAtEpochMs = System.currentTimeMillis(),
    )

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
            ?.forEach { directory ->
                if (directory.deleteRecursively()) Log.i(TAG, "Removed interrupted model installation staging directory.")
            }
    }

    private fun documentName(uri: android.net.Uri): String = application.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
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

    private fun copiedNamesSafe(directory: File, name: String) = !File(directory, name).exists()

    private fun copyDirectoryTree(treeUri: android.net.Uri, destinationRoot: File): List<String> {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val copied = mutableListOf<String>()
        fun copyChildren(parentId: String, relativeParent: String) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            application.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    require(ModelImportPolicy.isSafeFileName(name)) { "The selected directory contains an unsafe path." }
                    val relative = listOfNotNull(relativeParent.takeIf(String::isNotBlank), name).joinToString(File.separator)
                    val target = ModelImportPolicy.destination(destinationRoot, relative)
                    val documentId = cursor.getString(idIndex)
                    if (cursor.getString(mimeIndex) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        require(target.mkdirs() || target.isDirectory) { "Could not copy model directory $relative." }
                        copyChildren(documentId, relative)
                    } else {
                        require(target.parentFile?.mkdirs() != false) { "Could not prepare model directory." }
                        require(!target.exists()) { "The selected directory contains duplicate paths." }
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        application.contentResolver.openInputStream(documentUri).use { input ->
                            requireNotNull(input) { "A selected model file is no longer readable." }
                            FileOutputStream(target).use { output -> input.copyTo(output) }
                        }
                        copied += relative
                    }
                }
            } ?: error("The selected model directory is no longer readable.")
        }
        copyChildren(rootId, "")
        return copied
    }
    private companion object {
        const val TAG = "AiP123Models"
    }
}

internal data class InstalledModelLocation(val manifest: ModelManifest, val directory: File)
