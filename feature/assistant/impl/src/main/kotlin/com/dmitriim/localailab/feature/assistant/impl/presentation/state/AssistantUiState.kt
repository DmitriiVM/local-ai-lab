package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import androidx.annotation.StringRes
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.audio.input.model.AudioLevel
import com.dmitriim.localailab.core.ui.R as CoreUiR

data class AssistantUiState(
    val chatModels: List<ChatModelOption> = emptyList(),
    val speechModels: List<SpeechModelOption> = emptyList(),
    val voiceModels: List<TtsModelOption> = emptyList(),
    val selectedChatModelId: ModelId? = null,
    val selectedSpeechModelId: ModelId? = null,
    val selectedVoiceModelId: ModelId? = null,
    val selectedVoiceId: String? = null,
    val inputMode: AssistantInputMode = AssistantInputMode.DICTATE,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val chatSettings: ChatSettings = ChatSettings(),
    val speechInputSettings: SpeechInputSettings = SpeechInputSettings(),
    val speechOutputSettings: SpeechOutputSettings = SpeechOutputSettings(),
    val operation: AssistantOperation = AssistantOperation.Idle,
    val level: AudioLevel? = null,
    val speakingMessageId: String? = null,
    val contextUsage: ContextUsage? = null,
    val metrics: ChatMetrics? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val selectedChatModel: ChatModelOption? get() = chatModels.firstOrNull { it.id == selectedChatModelId }
    val selectedSpeechModel: SpeechModelOption? get() = speechModels.firstOrNull { it.id == selectedSpeechModelId }
    val selectedVoiceModel: TtsModelOption? get() = voiceModels.firstOrNull { it.id == selectedVoiceModelId }
    val compatibleVoices: List<TtsVoiceOption>
        get() = selectedVoiceModel?.compatibleVoices(speechOutputSettings.languageCode).orEmpty()
    val selectedVoice: TtsVoiceOption? get() = compatibleVoices.firstOrNull { it.id == selectedVoiceId }
    val isIdle: Boolean get() = operation == AssistantOperation.Idle
    val canSend: Boolean
        get() = isIdle &&
            input.isNotBlank() &&
            selectedChatModel?.let { model ->
                model.installed && model.capabilities != null
            } == true
    val canDictate: Boolean get() = isIdle && selectedSpeechModel?.installed == true

    @get:StringRes
    val voiceConfigurationError: Int?
        get() = when {
            selectedChatModel?.installed != true -> CoreUiR.string.assistant_error_select_chat_model
            selectedSpeechModel?.installed != true -> CoreUiR.string.assistant_error_select_speech_model
            selectedVoiceModel?.installed != true -> CoreUiR.string.assistant_error_select_tts_model
            selectedVoice == null -> CoreUiR.string.assistant_error_select_compatible_voice
            else -> null
        }
}
