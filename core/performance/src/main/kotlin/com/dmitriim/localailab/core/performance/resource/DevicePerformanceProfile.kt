package com.dmitriim.localailab.core.performance.resource

data class DevicePerformanceProfile(
    val manufacturer: String,
    val model: String,
    val hardware: String,
    val socManufacturer: String?,
    val socModel: String?,
    val sdkInt: Int,
    val abis: List<String>,
    val availableProcessors: Int,
)
