package com.dmitriim.localailab.feature.benchmark.api.domain

import com.dmitriim.localailab.ai.api.chat.LlmChatMessage
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput

sealed interface BenchmarkWorkload {
    val modelId: ModelId
    val modelDisplayName: String

    data class Chat(
        override val modelId: ModelId,
        override val modelDisplayName: String,
        val computePreference: ComputePreference,
        val messages: List<LlmChatMessage>,
        val maxTokens: Int,
        val temperature: Float,
        val topK: Int,
        val topP: Float,
        val seed: Int?,
        val contextSize: Int,
        val threadCount: Int,
    ) : BenchmarkWorkload

    data class SpeechToText(
        override val modelId: ModelId,
        override val modelDisplayName: String,
        val input: PcmAudioInput,
        val languageCode: String,
        val threadCount: String,
    ) : BenchmarkWorkload

    data class TextToSpeech(
        override val modelId: ModelId,
        override val modelDisplayName: String,
        val text: String,
        val languageCode: String,
        val voice: TextToSpeechVoiceCondition,
        val speed: Float,
        val sentenceSilenceScale: Float,
        val threadCount: Int,
    ) : BenchmarkWorkload
}
