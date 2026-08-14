package com.dmitriim.localailab.source.settings

enum class AudioRetention { SESSION_ONLY, LATEST_SUCCESSFUL, SEVEN_DAYS, THIRTY_DAYS, MANUAL }
enum class ThreadCountPolicy { ENGINE_DEFAULT, AVAILABLE_PROCESSORS, FIXED }
enum class ModelUnloadPolicy { WHEN_IDLE, WHEN_BACKGROUND, MANUAL }
enum class MetricDetail { STANDARD, VERBOSE }

data class AppSettings(
    val keepScreenAwake: Boolean = true,
    val confirmDestructiveActions: Boolean = true,
    val recordingRetention: AudioRetention = AudioRetention.SESSION_ONLY,
    val generatedAudioRetention: AudioRetention = AudioRetention.LATEST_SUCCESSFUL,
    val showAdvancedControls: Boolean = false,
    val threadCountPolicy: ThreadCountPolicy = ThreadCountPolicy.ENGINE_DEFAULT,
    val fixedThreadCount: Int = 0,
    val modelUnloadPolicy: ModelUnloadPolicy = ModelUnloadPolicy.WHEN_IDLE,
    val warmUpSelectedModel: Boolean = false,
    val metricDetail: MetricDetail = MetricDetail.STANDARD,
)
