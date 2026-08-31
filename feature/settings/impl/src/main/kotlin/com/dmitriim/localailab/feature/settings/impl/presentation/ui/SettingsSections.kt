package com.dmitriim.localailab.feature.settings.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.HuggingFaceTokenDialog
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.settings.api.domain.AppSettings
import com.dmitriim.localailab.feature.settings.api.domain.MetricDetail
import com.dmitriim.localailab.feature.settings.api.domain.ModelUnloadPolicy
import com.dmitriim.localailab.feature.settings.api.domain.ThreadCountPolicy
import com.dmitriim.localailab.feature.settings.impl.presentation.SettingsUiState

@Composable
internal fun PerformanceSettingsCard(
    settings: AppSettings,
    onUpdate: (((AppSettings) -> AppSettings)) -> Unit,
) {
    SettingsCard(
        stringResource(CoreUiR.string.settings_section_performance_defaults),
    ) {
        EnumRadioGroup(
            label = stringResource(CoreUiR.string.settings_thread_policy),
            selected = settings.threadCountPolicy,
            values = ThreadCountPolicy.entries,
            text = ThreadCountPolicy::label,
        ) { value -> onUpdate { it.copy(threadCountPolicy = value) } }
        EnumRadioGroup(
            label = stringResource(CoreUiR.string.settings_unload_models),
            selected = settings.modelUnloadPolicy,
            values = ModelUnloadPolicy.entries,
            text = ModelUnloadPolicy::label,
        ) { value -> onUpdate { it.copy(modelUnloadPolicy = value) } }
        Toggle(
            label = stringResource(CoreUiR.string.settings_warm_up_selected_model),
            checked = settings.warmUpSelectedModel,
        ) { value -> onUpdate { it.copy(warmUpSelectedModel = value) } }
        EnumSelector(
            label = stringResource(CoreUiR.string.settings_metric_detail),
            selected = settings.metricDetail,
            values = MetricDetail.entries,
            text = MetricDetail::label,
        ) { value -> onUpdate { it.copy(metricDetail = value) } }
    }
}

@Composable
internal fun AppearanceSettingsCard(
    settings: AppSettings,
    onUpdate: (((AppSettings) -> AppSettings)) -> Unit,
) {
    SettingsCard(stringResource(CoreUiR.string.settings_section_appearance)) {
        Toggle(
            label = stringResource(CoreUiR.string.settings_keep_screen_awake),
            checked = settings.keepScreenAwake,
        ) { value -> onUpdate { it.copy(keepScreenAwake = value) } }
        Toggle(
            label = stringResource(CoreUiR.string.settings_confirm_history_deletion),
            checked = settings.confirmDestructiveActions,
        ) { value -> onUpdate { it.copy(confirmDestructiveActions = value) } }
        Toggle(
            label = stringResource(CoreUiR.string.settings_show_advanced_controls),
            checked = settings.showAdvancedControls,
        ) { value -> onUpdate { it.copy(showAdvancedControls = value) } }
    }
}

@Composable
internal fun RetentionSettingsCard() {
    SettingsCard(stringResource(CoreUiR.string.settings_section_retention), purpleTonal = true) {
        Text(stringResource(CoreUiR.string.settings_settings_screen_115), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(CoreUiR.string.settings_settings_screen_116), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(CoreUiR.string.settings_settings_screen_117), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(CoreUiR.string.settings_settings_screen_118), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun ModelDownloadsSettingsCard(
    credentialStatus: HuggingFaceCredentialStatus,
    onRequestToken: () -> Unit,
    onClearToken: () -> Unit,
) {
    SettingsCard(stringResource(CoreUiR.string.settings_section_model_downloads), purpleTonal = true) {
        Text(stringResource(CoreUiR.string.settings_settings_screen_119), style = MaterialTheme.typography.titleSmall)
        when (credentialStatus) {
            HuggingFaceCredentialStatus.MISSING -> {
                Text(stringResource(CoreUiR.string.settings_settings_screen_120), style = MaterialTheme.typography.bodySmall)
                SettingsActionButton(CoreUiR.string.settings_settings_screen_121, onRequestToken)
            }
            HuggingFaceCredentialStatus.USER_CONFIGURED -> {
                Text(stringResource(CoreUiR.string.settings_settings_screen_122), style = MaterialTheme.typography.bodySmall)
                SettingsActionButton(CoreUiR.string.settings_settings_screen_123, onRequestToken)
                SettingsActionButton(CoreUiR.string.settings_settings_screen_124, onClearToken)
            }
            HuggingFaceCredentialStatus.DEBUG_CONFIGURED -> {
                Text(stringResource(CoreUiR.string.settings_settings_screen_125), style = MaterialTheme.typography.bodySmall)
                SettingsActionButton(CoreUiR.string.settings_settings_screen_126, onRequestToken)
            }
        }
    }
}

@Composable
private fun SettingsActionButton(label: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(label))
    }
}

@Composable
internal fun StoragePrivacySettingsCard(
    state: SettingsUiState,
    settings: AppSettings,
    onClearTemporaryMedia: () -> Unit,
    onRequestClearRunHistory: () -> Unit,
    onClearRunHistory: () -> Unit,
) {
    SettingsCard(stringResource(CoreUiR.string.settings_section_storage_privacy), purpleTonal = true) {
        StorageLine(stringResource(CoreUiR.string.settings_storage_models), state.storage.modelsBytes)
        StorageLine(stringResource(CoreUiR.string.settings_storage_temporary_recordings), state.storage.recordingsBytes)
        StorageLine(stringResource(CoreUiR.string.settings_storage_generated_audio), state.storage.generatedAudioBytes)
        StorageLine(stringResource(CoreUiR.string.settings_storage_history), state.storage.historyBytes)
        SettingsActionButton(CoreUiR.string.settings_settings_screen_127, onClearTemporaryMedia)
        SettingsActionButton(
            label = CoreUiR.string.settings_settings_screen_128,
            onClick = {
                if (settings.confirmDestructiveActions) {
                    onRequestClearRunHistory()
                } else {
                    onClearRunHistory()
                }
            },
        )
        Text(stringResource(CoreUiR.string.settings_settings_screen_129), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun SettingsDialogs(
    state: SettingsUiState,
    onDismissHistory: () -> Unit,
    onClearHistory: () -> Unit,
    onSaveToken: (String) -> Unit,
    onDismissToken: () -> Unit,
) {
    if (state.pendingRunHistoryClear) {
        AlertDialog(
            onDismissRequest = onDismissHistory,
            title = { Text(stringResource(CoreUiR.string.settings_settings_screen_107)) },
            text = { Text(stringResource(CoreUiR.string.settings_settings_screen_108)) },
            confirmButton = {
                OutlinedButton(onClick = onClearHistory) {
                    Text(stringResource(CoreUiR.string.settings_settings_screen_109))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissHistory) {
                    Text(stringResource(CoreUiR.string.settings_settings_screen_110))
                }
            },
        )
    }
    if (state.showHuggingFaceTokenDialog) {
        HuggingFaceTokenDialog(
            instruction = stringResource(CoreUiR.string.settings_hugging_face_token_dialog_104),
            saveLabel = stringResource(CoreUiR.string.settings_save_token),
            saving = state.isSavingHuggingFaceToken,
            error = state.huggingFaceTokenError,
            onSave = onSaveToken,
            onDismiss = onDismissToken,
        )
    }
}

@Composable
internal fun DeviceAndRuntimeCard(onClick: () -> Unit) {
    SettingsSurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(CoreUiR.string.settings_settings_screen_112), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(CoreUiR.string.settings_settings_screen_113), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(CoreUiR.string.settings_settings_screen_114), style = MaterialTheme.typography.headlineSmall)
        }
    }
}
