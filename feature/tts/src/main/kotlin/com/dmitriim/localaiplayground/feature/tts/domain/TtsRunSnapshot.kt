package com.dmitriim.localaiplayground.feature.tts.domain

import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.model.runs.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.runs.RunStatus

data class TtsRunSnapshot(
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
