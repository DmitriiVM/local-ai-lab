package com.dmitriim.localailab.source.settings

import android.app.Application
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.engine.ComputePreference
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreAssistantPreferencesRepository(
    application: Application,
) : AssistantPreferencesRepository {
    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { application.preferencesDataStoreFile("assistant-preferences") },
        migrations = listOf(
            LegacyChatDefaultsMigration(
                maxOutputTokensKey = CHAT_MAX_OUTPUT,
                contextSizeKey = CHAT_CONTEXT,
                migrationCompletedKey = CHAT_DEFAULTS_MIGRATED,
            ),
        ),
    )

    override val preferences: Flow<AssistantPreferences> = store.data.map { values ->
        AssistantPreferences(
            chat = AssistantChatPreferences(
                modelId = values[CHAT_MODEL],
                computePreference = values[CHAT_COMPUTE]
                    ?.let { stored -> ComputePreference.entries.firstOrNull { it.name == stored } }
                    ?: ComputePreference.CPU,
                systemPrompt = values[CHAT_SYSTEM_PROMPT] ?: AssistantChatPreferences().systemPrompt,
                temperature = values[CHAT_TEMPERATURE] ?: 0.7f,
                topK = values[CHAT_TOP_K] ?: 40,
                topP = values[CHAT_TOP_P] ?: 0.9f,
                maxOutputTokens = values[CHAT_MAX_OUTPUT] ?: 256,
                seed = values[CHAT_SEED]?.takeIf { it >= 0 },
                contextSize = values[CHAT_CONTEXT] ?: 2_048,
                threadCount = values[CHAT_THREADS] ?: 0,
            ),
            speechInput = AssistantSpeechInputPreferences(
                modelId = values[STT_MODEL],
                languageCode = values[STT_LANGUAGE] ?: "en",
                threadCount = values[STT_THREADS] ?: 0,
            ),
            speechOutput = AssistantSpeechOutputPreferences(
                modelId = values[TTS_MODEL],
                voiceId = values[TTS_VOICE],
                languageCode = values[TTS_LANGUAGE] ?: "en",
                speed = values[TTS_SPEED] ?: 1f,
                volume = values[TTS_VOLUME] ?: 1f,
                sentenceSilenceScale = values[TTS_SENTENCE_SILENCE] ?: 1f,
                threadCount = values[TTS_THREADS] ?: 0,
            ),
        )
    }

    override suspend fun update(preferences: AssistantPreferences) {
        store.edit { values ->
            values.putOrRemove(CHAT_MODEL, preferences.chat.modelId)
            values[CHAT_COMPUTE] = preferences.chat.computePreference.name
            values[CHAT_SYSTEM_PROMPT] = preferences.chat.systemPrompt
            values[CHAT_TEMPERATURE] = preferences.chat.temperature
            values[CHAT_TOP_K] = preferences.chat.topK
            values[CHAT_TOP_P] = preferences.chat.topP
            values[CHAT_MAX_OUTPUT] = preferences.chat.maxOutputTokens
            preferences.chat.seed?.let { values[CHAT_SEED] = it } ?: values.remove(CHAT_SEED)
            values[CHAT_CONTEXT] = preferences.chat.contextSize
            values[CHAT_THREADS] = preferences.chat.threadCount
            values.putOrRemove(STT_MODEL, preferences.speechInput.modelId)
            values[STT_LANGUAGE] = preferences.speechInput.languageCode
            values[STT_THREADS] = preferences.speechInput.threadCount
            values.putOrRemove(TTS_MODEL, preferences.speechOutput.modelId)
            values.putOrRemove(TTS_VOICE, preferences.speechOutput.voiceId)
            values[TTS_LANGUAGE] = preferences.speechOutput.languageCode
            values[TTS_SPEED] = preferences.speechOutput.speed
            values[TTS_VOLUME] = preferences.speechOutput.volume
            values[TTS_SENTENCE_SILENCE] = preferences.speechOutput.sentenceSilenceScale
            values[TTS_THREADS] = preferences.speechOutput.threadCount
        }
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val CHAT_MODEL = stringPreferencesKey("chat_model")
        val CHAT_COMPUTE = stringPreferencesKey("chat_compute")
        val CHAT_SYSTEM_PROMPT = stringPreferencesKey("chat_system_prompt")
        val CHAT_TEMPERATURE = floatPreferencesKey("chat_temperature")
        val CHAT_TOP_K = intPreferencesKey("chat_top_k")
        val CHAT_TOP_P = floatPreferencesKey("chat_top_p")
        val CHAT_MAX_OUTPUT = intPreferencesKey("chat_max_output")
        val CHAT_SEED = intPreferencesKey("chat_seed")
        val CHAT_CONTEXT = intPreferencesKey("chat_context")
        val CHAT_DEFAULTS_MIGRATED = booleanPreferencesKey("chat_defaults_migrated_v2")
        val CHAT_THREADS = intPreferencesKey("chat_threads")
        val STT_MODEL = stringPreferencesKey("stt_model")
        val STT_LANGUAGE = stringPreferencesKey("stt_language")
        val STT_THREADS = intPreferencesKey("stt_threads")
        val TTS_MODEL = stringPreferencesKey("tts_model")
        val TTS_VOICE = stringPreferencesKey("tts_voice")
        val TTS_LANGUAGE = stringPreferencesKey("tts_language")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_VOLUME = floatPreferencesKey("tts_volume")
        val TTS_SENTENCE_SILENCE = floatPreferencesKey("tts_sentence_silence")
        val TTS_THREADS = intPreferencesKey("tts_threads")
    }
}

internal class LegacyChatDefaultsMigration(
    private val maxOutputTokensKey: Preferences.Key<Int>,
    private val contextSizeKey: Preferences.Key<Int>,
    private val migrationCompletedKey: Preferences.Key<Boolean>,
) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[migrationCompletedKey] != true

    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        if (this[maxOutputTokensKey] == LEGACY_MAX_OUTPUT_TOKENS) {
            this[maxOutputTokensKey] = DEFAULT_MAX_OUTPUT_TOKENS
        }
        if (this[contextSizeKey] == LEGACY_CONTEXT_SIZE) {
            this[contextSizeKey] = DEFAULT_CONTEXT_SIZE
        }
        this[migrationCompletedKey] = true
    }

    override suspend fun cleanUp() = Unit

    private companion object {
        const val LEGACY_MAX_OUTPUT_TOKENS = 128
        const val DEFAULT_MAX_OUTPUT_TOKENS = 256
        const val LEGACY_CONTEXT_SIZE = 512
        const val DEFAULT_CONTEXT_SIZE = 2_048
    }
}
