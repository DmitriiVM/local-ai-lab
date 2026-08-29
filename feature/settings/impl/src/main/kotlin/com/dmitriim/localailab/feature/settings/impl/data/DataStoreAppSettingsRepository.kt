package com.dmitriim.localailab.feature.settings.impl.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.settings.api.data.AppSettingsRepository
import com.dmitriim.localailab.feature.settings.api.domain.AppSettings
import com.dmitriim.localailab.feature.settings.api.domain.AudioRetention
import com.dmitriim.localailab.feature.settings.api.domain.MetricDetail
import com.dmitriim.localailab.feature.settings.api.domain.ModelUnloadPolicy
import com.dmitriim.localailab.feature.settings.api.domain.ThreadCountPolicy
import com.dmitriim.localailab.feature.settings.api.domain.TtsSelectionPreferences
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreAppSettingsRepository(application: Application) : AppSettingsRepository {
    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { application.preferencesDataStoreFile("app-settings") },
    )

    override val settings: Flow<AppSettings> = store.data.map { values ->
        AppSettings(
            keepScreenAwake = values[KEEP_AWAKE] ?: true,
            confirmDestructiveActions = values[CONFIRM_DESTRUCTIVE] ?: true,
            recordingRetention = values[RECORDING_RETENTION]?.asEnum(AudioRetention.SESSION_ONLY) ?: AudioRetention.SESSION_ONLY,
            generatedAudioRetention = values[GENERATED_RETENTION]?.asEnum(AudioRetention.LATEST_SUCCESSFUL) ?: AudioRetention.LATEST_SUCCESSFUL,
            showAdvancedControls = values[SHOW_ADVANCED] ?: false,
            threadCountPolicy = values[THREAD_POLICY]?.asEnum(ThreadCountPolicy.ENGINE_DEFAULT) ?: ThreadCountPolicy.ENGINE_DEFAULT,
            fixedThreadCount = values[FIXED_THREADS] ?: 0,
            modelUnloadPolicy = values[UNLOAD_POLICY]?.asEnum(ModelUnloadPolicy.WHEN_IDLE) ?: ModelUnloadPolicy.WHEN_IDLE,
            warmUpSelectedModel = values[WARM_UP] ?: false,
            metricDetail = values[METRIC_DETAIL]?.asEnum(MetricDetail.STANDARD) ?: MetricDetail.STANDARD,
        )
    }

    override val ttsDraft: Flow<String?> = store.data.map { values -> values[TTS_DRAFT] }
    override val ttsSelection: Flow<TtsSelectionPreferences> = store.data.map { values ->
        TtsSelectionPreferences(
            selectedModelId = values[TTS_SELECTED_MODEL],
            voiceIdsByModel = values[TTS_VOICE_SELECTIONS].orEmpty().mapNotNull(TtsVoiceSelectionCodec::decode).toMap(),
        )
    }
    override val sttSelectedModel: Flow<String?> = store.data.map { values -> values[STT_SELECTED_MODEL] }

    override suspend fun update(settings: AppSettings) {
        store.edit { values ->
            values[KEEP_AWAKE] = settings.keepScreenAwake
            values[CONFIRM_DESTRUCTIVE] = settings.confirmDestructiveActions
            values[RECORDING_RETENTION] = settings.recordingRetention.name
            values[GENERATED_RETENTION] = settings.generatedAudioRetention.name
            values[SHOW_ADVANCED] = settings.showAdvancedControls
            values[THREAD_POLICY] = settings.threadCountPolicy.name
            values[FIXED_THREADS] = settings.fixedThreadCount.coerceIn(0, 64)
            values[UNLOAD_POLICY] = settings.modelUnloadPolicy.name
            values[WARM_UP] = settings.warmUpSelectedModel
            values[METRIC_DETAIL] = settings.metricDetail.name
        }
    }

    override suspend fun updateTtsDraft(text: String) {
        store.edit { values -> values[TTS_DRAFT] = text }
    }

    override suspend fun updateTtsSelectedModel(modelId: String) {
        store.edit { values -> values[TTS_SELECTED_MODEL] = modelId }
    }

    override suspend fun updateTtsVoice(modelId: String, voiceId: String) {
        store.edit { values ->
            val selections = values[TTS_VOICE_SELECTIONS].orEmpty()
                .mapNotNull(TtsVoiceSelectionCodec::decode)
                .toMap()
                .toMutableMap()
                .apply { put(modelId, voiceId) }
            values[TTS_VOICE_SELECTIONS] = selections.mapTo(mutableSetOf()) { (savedModelId, savedVoiceId) ->
                TtsVoiceSelectionCodec.encode(savedModelId, savedVoiceId)
            }
        }
    }

    override suspend fun clearTtsVoice(modelId: String) {
        store.edit { values ->
            val selections = values[TTS_VOICE_SELECTIONS].orEmpty()
                .mapNotNull(TtsVoiceSelectionCodec::decode)
                .toMap()
                .toMutableMap()
                .apply { remove(modelId) }
            values[TTS_VOICE_SELECTIONS] = selections.mapTo(mutableSetOf()) { (savedModelId, savedVoiceId) ->
                TtsVoiceSelectionCodec.encode(savedModelId, savedVoiceId)
            }
        }
    }

    override suspend fun updateSttSelectedModel(modelId: String) {
        store.edit { values -> values[STT_SELECTED_MODEL] = modelId }
    }

    private inline fun <reified T : Enum<T>> String.asEnum(default: T): T = enumValues<T>().firstOrNull { it.name == this } ?: default

    private companion object {
        val KEEP_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val CONFIRM_DESTRUCTIVE = booleanPreferencesKey("confirm_destructive_actions")
        val RECORDING_RETENTION = stringPreferencesKey("recording_retention")
        val GENERATED_RETENTION = stringPreferencesKey("generated_retention")
        val SHOW_ADVANCED = booleanPreferencesKey("show_advanced_controls")
        val THREAD_POLICY = stringPreferencesKey("thread_count_policy")
        val FIXED_THREADS = intPreferencesKey("fixed_thread_count")
        val UNLOAD_POLICY = stringPreferencesKey("model_unload_policy")
        val WARM_UP = booleanPreferencesKey("warm_up_selected_model")
        val METRIC_DETAIL = stringPreferencesKey("metric_detail")
        val TTS_DRAFT = stringPreferencesKey("tts_draft")
        val TTS_SELECTED_MODEL = stringPreferencesKey("tts_selected_model")
        val TTS_VOICE_SELECTIONS = stringSetPreferencesKey("tts_voice_selections")
        val STT_SELECTED_MODEL = stringPreferencesKey("stt_selected_model")
    }
}
