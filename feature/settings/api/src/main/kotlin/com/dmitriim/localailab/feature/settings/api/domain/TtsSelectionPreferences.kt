package com.dmitriim.localailab.feature.settings.api.domain

data class TtsSelectionPreferences(
    val selectedModelId: String? = null,
    val voiceIdsByModel: Map<String, String> = emptyMap(),
)
