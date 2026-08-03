package com.dmitriim.localaiplayground.feature.tts.presentation

import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object TtsReplayRestorer {
    fun restore(state: TextToSpeechUiState, run: RunRecord): TtsReplayRestoreResult {
        val replayModelId = run.model?.modelId?.let(::ModelId)
        val installedModelId = replayModelId?.takeIf { candidate ->
            state.models.any { it.id == candidate && it.installed }
        }
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        val replayLanguage = parameters?.get("language")?.jsonPrimitive?.content
            ?.let { code -> TtsLanguage.entries.firstOrNull { it.code == code } }
        val replayVoiceId = parameters?.get("voiceId")?.jsonPrimitive?.content
        val replaySpeakerId = parameters?.get("speakerId")?.jsonPrimitive?.intOrNull
        val selectedModelId = installedModelId ?: state.selectedModelId
        val selectedModel = state.models.firstOrNull { it.id == selectedModelId }
        val language = if (selectedModel?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
            TtsLanguage.ENGLISH
        } else {
            replayLanguage ?: state.language
        }
        val voices = TtsVoiceResolver.forModel(selectedModel, language, state.referenceVoices)
        val selectedVoiceId = if (replayModelId == null || installedModelId != null) {
            replayVoiceId
                ?.takeIf { id -> voices.any { it.id == id } }
                ?: replaySpeakerId
                    ?.let { speakerId -> voices.firstOrNull { it.speakerId == speakerId }?.id }
                ?: state.selectedVoiceId?.takeIf { id -> voices.any { it.id == id } }
                ?: voices.firstOrNull()?.id
        } else {
            state.selectedVoiceId?.takeIf { id -> voices.any { it.id == id } }
                ?: voices.firstOrNull()?.id
        }
        return TtsReplayRestoreResult(
            state = state.copy(
                selectedModelId = selectedModelId,
                selectedVoiceId = selectedVoiceId,
                text = run.input?.take(state.characterLimit) ?: state.text,
                language = language,
                speed = parameters?.get("speed")?.jsonPrimitive?.floatOrNull ?: state.speed,
                sentenceSilenceScale = parameters?.get("sentenceSilenceScale")?.jsonPrimitive?.floatOrNull
                    ?: state.sentenceSilenceScale,
                volume = parameters?.get("volume")?.jsonPrimitive?.floatOrNull ?: state.volume,
                threadCount = parameters?.get("threadCount")?.jsonPrimitive?.content ?: state.threadCount,
                audioEffects = state.audioEffects.copy(
                    pitchSemitones = parameters?.get("pitchSemitones")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.pitchSemitones,
                    formantSemitones = parameters?.get("formantSemitones")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.formantSemitones,
                    lowEqDb = parameters?.get("lowEqDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.lowEqDb,
                    midEqDb = parameters?.get("midEqDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.midEqDb,
                    highEqDb = parameters?.get("highEqDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.highEqDb,
                    saturationDriveDb = parameters?.get("saturationDriveDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.saturationDriveDb,
                ),
                errorMessage = when {
                    replayModelId != null && installedModelId == null ->
                        "Saved model ${run.model?.displayName.orEmpty()} is no longer installed. Select a compatible model before synthesizing."
                    selectedModel?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO &&
                        replayVoiceId != null &&
                        voices.none { it.id == replayVoiceId } ->
                        "The saved reference voice was deleted. Record or import another reference before synthesizing."
                    else -> null
                },
            ),
            selectedModelIdToPersist = installedModelId,
        )
    }
}

internal data class TtsReplayRestoreResult(
    val state: TextToSpeechUiState,
    val selectedModelIdToPersist: ModelId?,
)
