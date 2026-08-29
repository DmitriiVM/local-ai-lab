package com.dmitriim.localailab.feature.settings.api.data

import com.dmitriim.localailab.feature.settings.api.domain.AppSettings
import com.dmitriim.localailab.feature.settings.api.domain.TtsSelectionPreferences
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    val ttsDraft: Flow<String?>
    val ttsSelection: Flow<TtsSelectionPreferences>
    val sttSelectedModel: Flow<String?>

    suspend fun update(settings: AppSettings)
    suspend fun updateTtsDraft(text: String)
    suspend fun updateTtsSelectedModel(modelId: String)
    suspend fun updateTtsVoice(modelId: String, voiceId: String)
    suspend fun clearTtsVoice(modelId: String)
    suspend fun updateSttSelectedModel(modelId: String)
}
