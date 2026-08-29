package com.dmitriim.localailab.feature.tts.domain

import com.dmitriim.localailab.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.core.voice.tts.SpeechSynthesisMetrics

data class TtsRunSnapshot(
    val runId: String,
    val status: RunStatus,
    val startedAtEpochMs: Long,
    val model: RunModelSnapshot?,
    val input: String,
    val languageCode: String,
    val voiceId: String,
    val voiceName: String,
    val speakerId: Int?,
    val referenceVoiceId: String?,
    val referenceVoiceName: String?,
    val watermarkStatus: String,
    val speed: Float,
    val sentenceSilenceScale: Float,
    val volume: Float,
    val threadCount: String,
    val audioEffects: SpeechAudioEffects,
    val metrics: SpeechSynthesisMetrics?,
    val errorMessage: String?,
)
