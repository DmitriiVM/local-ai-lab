package com.dmitriim.localailab.feature.assistant.impl.presentation.ui.tts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.TtsModelOption

@Composable
internal fun AssistantSpeakSettingsSheet(
    models: List<TtsModelOption>,
    selectedModelId: ModelId?,
    selectedVoiceId: String?,
    settings: SpeechOutputSettings,
    enabled: Boolean,
    onApply: (ModelId, String, SpeechOutputSettings) -> String?,
    onPreview: (ModelId, String, SpeechOutputSettings) -> String?,
    onOpenModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftModelId by remember(selectedModelId) { mutableStateOf(selectedModelId) }
    var draftVoiceId by remember(selectedVoiceId) { mutableStateOf(selectedVoiceId) }
    var draft by remember(settings) { mutableStateOf(settings) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectingSpeechModel by remember { mutableStateOf(false) }
    val selectSpeechModelError = stringResource(CoreUiR.string.assistant_error_select_speech_model)
    val selectCompatibleVoiceError = stringResource(CoreUiR.string.assistant_error_select_compatible_voice)
    val commit = { modelId: ModelId?, voiceId: String?, candidate: SpeechOutputSettings ->
        error = when {
            modelId == null -> selectSpeechModelError
            voiceId == null -> selectCompatibleVoiceError
            else -> onApply(modelId, voiceId, candidate)
        }
    }
    val selectSpeechModel: (ModelId) -> Unit = { modelId ->
        val model = models.firstOrNull { it.id == modelId }
        val selection = speechModelSelection(model, draft)
        draftModelId = modelId
        draft = selection.settings
        draftVoiceId = selection.voiceId
        commit(modelId, selection.voiceId, selection.settings)
    }
    val selectedModel = models.firstOrNull { it.id == draftModelId }
    SpeakSettingsSheetBody(
        selectingSpeechModel = selectingSpeechModel,
        models = models,
        selectedModel = selectedModel,
        draftModelId = draftModelId,
        draftVoiceId = draftVoiceId,
        draft = draft,
        error = error,
        enabled = enabled,
        onOpenModels = onOpenModels,
        onSelectSpeechModel = { modelId ->
            selectSpeechModel(modelId)
            selectingSpeechModel = false
        },
        onBackToSettings = { selectingSpeechModel = false },
        onChooseModel = { selectingSpeechModel = true },
        onLanguageChange = { language, voiceId ->
            draftVoiceId = voiceId
            val candidate = draft.copy(languageCode = language)
            draft = candidate
            commit(draftModelId, voiceId, candidate)
        },
        onVoiceChange = { voiceId ->
            draftVoiceId = voiceId
            commit(draftModelId, voiceId, draft)
        },
        onChange = { candidate ->
            draft = candidate
            commit(draftModelId, draftVoiceId, candidate)
        },
        onReset = {
            val candidate = SpeechOutputSettings()
            val voiceId = selectedModel?.compatibleVoices(candidate.languageCode)?.firstOrNull()?.id
            draft = candidate
            draftVoiceId = voiceId
            commit(draftModelId, voiceId, candidate)
        },
        onPreview = {
            val modelId = draftModelId
            val voiceId = draftVoiceId
            error = when {
                modelId == null -> selectSpeechModelError
                voiceId == null -> selectCompatibleVoiceError
                else -> onPreview(modelId, voiceId, draft)
            }
        },
        onDismiss = onDismiss,
    )
}
