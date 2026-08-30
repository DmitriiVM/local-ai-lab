package com.dmitriim.localailab.feature.assistant.impl.presentation.ui.tts

import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.TtsModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.normalizeLanguageCode

internal data class SpeechModelSelection(
    val settings: SpeechOutputSettings,
    val voiceId: String?,
)

internal fun speechModelSelection(
    model: TtsModelOption?,
    settings: SpeechOutputSettings,
): SpeechModelSelection {
    val language = assistantTtsLanguages.firstOrNull { candidate ->
        model?.languages?.isEmpty() == true ||
            model?.languages?.any { normalizeLanguageCode(it) == candidate.code } == true
    }?.code ?: "en"
    return SpeechModelSelection(
        settings = settings.copy(languageCode = language),
        voiceId = model?.compatibleVoices(language)?.firstOrNull()?.id,
    )
}

internal data class AssistantTtsLanguage(val code: String, val labelRes: Int)

internal val assistantTtsLanguages = listOf(
    AssistantTtsLanguage("en", CoreUiR.string.language_english),
    AssistantTtsLanguage("ru", CoreUiR.string.language_russian),
    AssistantTtsLanguage("zh", CoreUiR.string.language_chinese),
)
