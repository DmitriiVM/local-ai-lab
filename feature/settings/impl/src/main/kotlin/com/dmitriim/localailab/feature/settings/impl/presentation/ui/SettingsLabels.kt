package com.dmitriim.localailab.feature.settings.impl.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.settings.api.domain.MetricDetail
import com.dmitriim.localailab.feature.settings.api.domain.ModelUnloadPolicy
import com.dmitriim.localailab.feature.settings.api.domain.ThreadCountPolicy

@Composable
internal fun Long.readable(): String = when {
    this < 1_024 -> stringResource(CoreUiR.string.settings_storage_bytes, this)
    this < 1_048_576 -> stringResource(CoreUiR.string.settings_storage_kilobytes, this / 1_024)
    else -> stringResource(CoreUiR.string.settings_storage_megabytes, this / 1_048_576.0)
}

@Composable
internal fun ThreadCountPolicy.label(): String = stringResource(
    when (this) {
        ThreadCountPolicy.ENGINE_DEFAULT -> CoreUiR.string.settings_thread_policy_engine_default
        ThreadCountPolicy.AVAILABLE_PROCESSORS -> CoreUiR.string.settings_thread_policy_available_processors
        ThreadCountPolicy.FIXED -> CoreUiR.string.settings_thread_policy_fixed
    },
)

@Composable
internal fun ModelUnloadPolicy.label(): String = stringResource(
    when (this) {
        ModelUnloadPolicy.WHEN_IDLE -> CoreUiR.string.settings_model_unload_when_idle
        ModelUnloadPolicy.WHEN_BACKGROUND -> CoreUiR.string.settings_model_unload_when_background
        ModelUnloadPolicy.MANUAL -> CoreUiR.string.settings_model_unload_manual
    },
)

@Composable
internal fun MetricDetail.label(): String = stringResource(
    when (this) {
        MetricDetail.STANDARD -> CoreUiR.string.settings_metric_detail_standard
        MetricDetail.VERBOSE -> CoreUiR.string.settings_metric_detail_verbose
    },
)
