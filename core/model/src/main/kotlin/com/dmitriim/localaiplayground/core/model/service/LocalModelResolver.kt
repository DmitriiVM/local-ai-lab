package com.dmitriim.localaiplayground.core.model.service

import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.runtime.ChatModelReference
import com.dmitriim.localaiplayground.core.model.runtime.SpeechToTextModelReference
import com.dmitriim.localaiplayground.core.model.runtime.TextToSpeechModelReference

/** Resolves validated app-private model locations for the runtime-specific features. */
interface LocalModelResolver {
    suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference>
    suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference>
    suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference>
}
