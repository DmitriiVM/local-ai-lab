package com.dmitriim.localailab.core.model.service

import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.runtime.ChatModelReference
import com.dmitriim.localailab.core.model.runtime.SpeechToTextModelReference
import com.dmitriim.localailab.core.model.runtime.TextToSpeechModelReference

/** Resolves validated app-private model locations for the runtime-specific features. */
interface LocalModelResolver {
    suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference>
    suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference>
    suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference>
}
