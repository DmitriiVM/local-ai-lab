package com.dmitriim.localailab.feature.settings.api.data

import com.dmitriim.localailab.feature.settings.api.domain.AssistantPreferences
import kotlinx.coroutines.flow.Flow

interface AssistantPreferencesRepository {
    val preferences: Flow<AssistantPreferences>

    suspend fun update(preferences: AssistantPreferences)
}
