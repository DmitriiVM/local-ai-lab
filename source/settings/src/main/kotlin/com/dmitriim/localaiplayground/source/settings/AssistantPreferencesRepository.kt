package com.dmitriim.localaiplayground.source.settings

import kotlinx.coroutines.flow.Flow

interface AssistantPreferencesRepository {
    val preferences: Flow<AssistantPreferences>

    suspend fun update(preferences: AssistantPreferences)
}
