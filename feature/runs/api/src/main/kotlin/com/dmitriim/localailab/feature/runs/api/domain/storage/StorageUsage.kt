package com.dmitriim.localailab.feature.runs.api.domain.storage

data class StorageUsage(
    val modelsBytes: Long = 0,
    val recordingsBytes: Long = 0,
    val generatedAudioBytes: Long = 0,
    val historyBytes: Long = 0,
)
