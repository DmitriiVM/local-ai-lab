package com.dmitriim.localaiplayground.source.settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    val ttsDraft: Flow<String?>

    suspend fun update(settings: AppSettings)
    suspend fun updateTtsDraft(text: String)
}
