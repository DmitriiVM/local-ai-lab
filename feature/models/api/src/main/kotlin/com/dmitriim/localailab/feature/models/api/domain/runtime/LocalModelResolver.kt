package com.dmitriim.localailab.feature.models.api.domain.runtime

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.runtime.ChatModelReference

/** Resolves validated app-private model locations for the runtime-specific features. */
interface LocalModelResolver {
    suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference>
    suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference>
    suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference>
}
