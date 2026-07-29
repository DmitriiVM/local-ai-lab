package com.dmitriim.localaiplayground.core.model

import kotlinx.coroutines.flow.Flow

/** Owns the user-visible library of installed local models. */
interface ModelLibrary {
    val installedModels: Flow<List<InstalledModel>>

    suspend fun import(request: ModelImportRequest): Result<ModelId>
    suspend fun validate(modelId: ModelId): Result<InstalledModel>
    suspend fun delete(modelId: ModelId): Result<Unit>
}

/** Exposes the bundled catalog and its persisted download operations. */
interface ModelTransfers {
    val catalog: Flow<List<CatalogModel>>
    val transfers: Flow<Map<ModelId, ModelTransferState>>

    suspend fun download(modelId: ModelId): Result<Unit>
    suspend fun cancelTransfer(modelId: ModelId)
}

/** Resolves validated app-private model locations for the runtime-specific features. */
interface LocalModelResolver {
    suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference>
    suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference>
    suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference>
}

/** Checks model requirements against the device and reports local model health. */
interface ModelDiagnostics {
    suspend fun compatibility(model: ModelManifest): ModelCompatibility
    suspend fun runDiagnostics(): DeviceDiagnostics
}

data class ChatModelReference(
    val modelId: ModelId,
    val displayName: String,
    val profileType: ModelProfileId,
    val modelPath: String,
    val defaultContextSize: Int,
)

data class SpeechToTextModelReference(
    val modelId: ModelId,
    val displayName: String,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val sampleRateHz: Int,
    val languages: Set<String>,
)

data class TextToSpeechModelReference(
    val modelId: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val sampleRateHz: Int,
    val languages: Set<String>,
    val speakerCount: Int?,
    val voiceMode: TtsVoiceMode,
    val supportedControls: Set<TtsControl>,
)
