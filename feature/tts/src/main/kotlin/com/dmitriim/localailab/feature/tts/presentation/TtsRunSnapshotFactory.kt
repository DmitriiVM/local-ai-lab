package com.dmitriim.localailab.feature.tts.presentation

import com.dmitriim.localailab.core.model.runs.RunModelSnapshot
import com.dmitriim.localailab.core.model.runs.RunStatus
import com.dmitriim.localailab.core.voice.tts.SpeechSynthesisMetrics
import com.dmitriim.localailab.feature.tts.domain.TtsRunSnapshot

internal object TtsRunSnapshotFactory {
    fun create(
        runId: String,
        status: RunStatus,
        startedAt: Long,
        model: TtsModelOption?,
        state: TextToSpeechUiState,
        metrics: SpeechSynthesisMetrics?,
        error: String?,
    ): TtsRunSnapshot {
        val voice = requireNotNull(state.selectedVoice)
        return TtsRunSnapshot(
            runId = runId,
            status = status,
            startedAtEpochMs = startedAt,
            model = model?.let { RunModelSnapshot(it.id.value, it.displayName, it.engineId.value) },
            input = state.text,
            languageCode = state.language.code,
            voiceId = voice.id,
            voiceName = voice.displayName,
            speakerId = voice.speakerId,
            referenceVoiceId = voice.reference?.id,
            referenceVoiceName = voice.reference?.displayName,
            watermarkStatus = if (state.usesReferenceVoice) "NOT_WATERMARKED" else "NOT_APPLICABLE",
            speed = state.speed,
            sentenceSilenceScale = state.sentenceSilenceScale,
            volume = state.volume,
            threadCount = state.threadCount,
            audioEffects = state.audioEffects,
            metrics = metrics,
            errorMessage = error,
        )
    }
}
