package com.dmitriim.localaiplayground.source.settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(settings: AppSettings)
}
