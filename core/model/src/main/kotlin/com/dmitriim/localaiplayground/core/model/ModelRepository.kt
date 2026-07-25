package com.dmitriim.localaiplayground.core.model

import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    val installedModels: Flow<List<InstalledModel>>
    val catalog: Flow<List<CatalogModel>>
    val transfers: Flow<Map<ModelId, ModelTransferState>>

    suspend fun import(request: ModelImportRequest): Result<ModelId>
    suspend fun download(modelId: ModelId): Result<Unit>
    suspend fun cancelTransfer(modelId: ModelId)
    suspend fun validate(modelId: ModelId): Result<InstalledModel>
    /** Resolves an app-private primary GGUF path after revalidating the installed model. */
    suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference>
    /** Resolves a validated app-private STT model directory. */
    suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference>
    suspend fun compatibility(model: ModelManifest): ModelCompatibility
    suspend fun load(modelId: ModelId): Result<Unit>
    suspend fun unload(modelId: ModelId): Result<Unit>
    suspend fun delete(modelId: ModelId): Result<Unit>
    suspend fun runDiagnostics(): DeviceDiagnostics
}

data class ChatModelReference(
    val modelId: ModelId,
    val displayName: String,
    val modelPath: String,
    val defaultContextSize: Int,
)

data class SpeechToTextModelReference(
    val modelId: ModelId,
    val displayName: String,
    val modelDirectory: String,
    val sampleRateHz: Int,
    val languages: Set<String>,
)
