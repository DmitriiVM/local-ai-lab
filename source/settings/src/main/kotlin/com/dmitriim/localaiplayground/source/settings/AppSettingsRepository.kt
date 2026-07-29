package com.dmitriim.localaiplayground.source.settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    val ttsDraft: Flow<String?>
    val ttsSelection: Flow<TtsSelectionPreferences>

    suspend fun update(settings: AppSettings)
    suspend fun updateTtsDraft(text: String)
    suspend fun updateTtsSelectedModel(modelId: String)
    suspend fun updateTtsVoice(modelId: String, voiceId: String)
    suspend fun clearTtsVoice(modelId: String)
}

data class TtsSelectionPreferences(
    val selectedModelId: String? = null,
    val voiceIdsByModel: Map<String, String> = emptyMap(),
)
