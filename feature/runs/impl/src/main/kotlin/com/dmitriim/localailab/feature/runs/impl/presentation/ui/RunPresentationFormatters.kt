package com.dmitriim.localailab.feature.runs.impl.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.runs.api.domain.history.RunKind
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus

@Composable
internal fun AiCapability.label(): String = stringResource(
    when (this) {
        AiCapability.CHAT -> CoreUiR.string.runs_capability_chat
        AiCapability.SPEECH_TO_TEXT -> CoreUiR.string.runs_capability_speech_to_text
        AiCapability.TEXT_TO_SPEECH -> CoreUiR.string.runs_capability_text_to_speech
        AiCapability.VOICE_ACTIVITY_DETECTION -> CoreUiR.string.runs_capability_voice_activity_detection
        AiCapability.VOICE_ASSISTANT -> CoreUiR.string.runs_capability_voice_assistant
    },
)

@Composable
internal fun AiCapability.filterLabel(): String = stringResource(
    when (this) {
        AiCapability.SPEECH_TO_TEXT -> CoreUiR.string.runs_filter_stt
        AiCapability.TEXT_TO_SPEECH -> CoreUiR.string.runs_filter_tts
        AiCapability.VOICE_ASSISTANT -> CoreUiR.string.runs_filter_assistant
        else -> return label()
    },
)

@Composable
internal fun RunStatus.label(): String = stringResource(
    when (this) {
        RunStatus.SUCCEEDED -> CoreUiR.string.runs_status_succeeded
        RunStatus.CANCELLED -> CoreUiR.string.runs_status_cancelled
        RunStatus.FAILED -> CoreUiR.string.runs_status_failed
    },
)

@Composable
internal fun RunKind.label(): String = stringResource(
    when (this) {
        RunKind.INFERENCE -> CoreUiR.string.runs_kind_inference
        RunKind.BENCHMARK_SESSION -> CoreUiR.string.runs_kind_benchmark_session
    },
)
