package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel

sealed interface VoicePipelineEvent {
    data class Prepared(val pipeline: VoicePipelineInfo, val componentIds: VoiceComponentRunIds) : VoicePipelineEvent
    data class Phase(val value: VoiceTurnPhase) : VoicePipelineEvent
    data class Level(val value: AudioLevel) : VoicePipelineEvent
    data class FinalTranscript(val value: String) : VoicePipelineEvent
    data class ContextPrepared(val value: VoiceContextUsage) : VoicePipelineEvent
    data class AssistantToken(val value: String) : VoicePipelineEvent
    data class AssistantCompleted(val value: String) : VoicePipelineEvent
    data class Completed(val metrics: VoicePipelineMetrics) : VoicePipelineEvent
}
