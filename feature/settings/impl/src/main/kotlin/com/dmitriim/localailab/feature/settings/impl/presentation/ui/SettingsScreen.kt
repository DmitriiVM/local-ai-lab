package com.dmitriim.localailab.feature.settings.impl.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSectionCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceTone
import com.dmitriim.localailab.core.ui.component.HuggingFaceTokenDialog
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.settings.api.domain.AppSettings
import com.dmitriim.localailab.feature.settings.api.domain.MetricDetail
import com.dmitriim.localailab.feature.settings.api.domain.ModelUnloadPolicy
import com.dmitriim.localailab.feature.settings.api.domain.ThreadCountPolicy
import com.dmitriim.localailab.feature.settings.impl.presentation.SettingsUiState

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenDeviceAndRuntimes: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onClearTemporaryMedia: () -> Unit,
    onRequestClearRunHistory: () -> Unit,
    onDismissClearRunHistory: () -> Unit,
    onClearRunHistory: () -> Unit,
    onRequestHuggingFaceToken: () -> Unit,
    onDismissHuggingFaceToken: () -> Unit,
    onSaveHuggingFaceToken: (String) -> Unit,
    onClearHuggingFaceToken: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val settings = state.settings
    SettingsDialogs(
        state = state,
        onDismissHistory = onDismissClearRunHistory,
        onClearHistory = onClearRunHistory,
        onSaveToken = onSaveHuggingFaceToken,
        onDismissToken = onDismissHuggingFaceToken,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = dimensions.screenPadding,
                top = dimensions.topBarOverlayClearance + 20.dp,
                end = dimensions.screenPadding,
                bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(dimensions.sectionSpacing),
    ) {
        Text(
            text = stringResource(CoreUiR.string.settings_settings_screen_111),
            style = MaterialTheme.typography.headlineMedium,
        )
        DeviceAndRuntimeCard(onClick = onOpenDeviceAndRuntimes)
        if (settings.showAdvancedControls) {
            SettingsCard(
                stringResource(CoreUiR.string.settings_section_performance_defaults),
                styled = false,
            ) {
                EnumRadioGroup(
                    label = stringResource(CoreUiR.string.settings_thread_policy),
                    selected = settings.threadCountPolicy,
                    values = ThreadCountPolicy.entries,
                    text = ThreadCountPolicy::label,
                ) { value ->
                    onUpdate { it.copy(threadCountPolicy = value) }
                }
                EnumRadioGroup(
                    label = stringResource(CoreUiR.string.settings_unload_models),
                    selected = settings.modelUnloadPolicy,
                    values = ModelUnloadPolicy.entries,
                    text = ModelUnloadPolicy::label,
                ) { value ->
                    onUpdate { it.copy(modelUnloadPolicy = value) }
                }
                Toggle(
                    label = stringResource(CoreUiR.string.settings_warm_up_selected_model),
                    checked = settings.warmUpSelectedModel,
                ) { value ->
                    onUpdate { it.copy(warmUpSelectedModel = value) }
                }
                EnumSelector(
                    label = stringResource(CoreUiR.string.settings_metric_detail),
                    selected = settings.metricDetail,
                    values = MetricDetail.entries,
                    text = MetricDetail::label,
                ) { value ->
                    onUpdate { it.copy(metricDetail = value) }
                }
            }
        }
        SettingsCard(stringResource(CoreUiR.string.settings_section_appearance), styled = false) {
            Toggle(
                label = stringResource(CoreUiR.string.settings_keep_screen_awake),
                checked = settings.keepScreenAwake,
            ) { value ->
                onUpdate { it.copy(keepScreenAwake = value) }
            }
            Toggle(
                label = stringResource(CoreUiR.string.settings_confirm_history_deletion),
                checked = settings.confirmDestructiveActions,
            ) { value ->
                onUpdate { it.copy(confirmDestructiveActions = value) }
            }
            Toggle(
                label = stringResource(CoreUiR.string.settings_show_advanced_controls),
                checked = settings.showAdvancedControls,
            ) { value ->
                onUpdate { it.copy(showAdvancedControls = value) }
            }
        }
        SettingsCard(stringResource(CoreUiR.string.settings_section_retention), purpleTonal = true) {
            Text(
                text = stringResource(CoreUiR.string.settings_settings_screen_115),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(CoreUiR.string.settings_settings_screen_116),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(CoreUiR.string.settings_settings_screen_117),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(CoreUiR.string.settings_settings_screen_118),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SettingsCard(stringResource(CoreUiR.string.settings_section_model_downloads), purpleTonal = true) {
            Text(
                text = stringResource(CoreUiR.string.settings_settings_screen_119),
                style = MaterialTheme.typography.titleSmall,
            )
            when (state.huggingFaceCredentialStatus) {
                HuggingFaceCredentialStatus.MISSING -> {
                    Text(
                        text = stringResource(CoreUiR.string.settings_settings_screen_120),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = onRequestHuggingFaceToken,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(CoreUiR.string.settings_settings_screen_121))
                    }
                }
                HuggingFaceCredentialStatus.USER_CONFIGURED -> {
                    Text(
                        text = stringResource(CoreUiR.string.settings_settings_screen_122),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = onRequestHuggingFaceToken,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(CoreUiR.string.settings_settings_screen_123))
                    }
                    OutlinedButton(
                        onClick = onClearHuggingFaceToken,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(CoreUiR.string.settings_settings_screen_124))
                    }
                }
                HuggingFaceCredentialStatus.DEBUG_CONFIGURED -> {
                    Text(
                        text = stringResource(CoreUiR.string.settings_settings_screen_125),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = onRequestHuggingFaceToken,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(CoreUiR.string.settings_settings_screen_126))
                    }
                }
            }
        }
        SettingsCard(stringResource(CoreUiR.string.settings_section_storage_privacy), purpleTonal = true) {
            StorageLine(stringResource(CoreUiR.string.settings_storage_models), state.storage.modelsBytes)
            StorageLine(
                stringResource(CoreUiR.string.settings_storage_temporary_recordings),
                state.storage.recordingsBytes,
            )
            StorageLine(
                stringResource(CoreUiR.string.settings_storage_generated_audio),
                state.storage.generatedAudioBytes,
            )
            StorageLine(stringResource(CoreUiR.string.settings_storage_history), state.storage.historyBytes)
            OutlinedButton(
                onClick = onClearTemporaryMedia,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(CoreUiR.string.settings_settings_screen_127))
            }
            OutlinedButton(
                onClick = {
                    if (settings.confirmDestructiveActions) {
                        onRequestClearRunHistory()
                    } else {
                        onClearRunHistory()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(CoreUiR.string.settings_settings_screen_128))
            }
            Text(
                text = stringResource(CoreUiR.string.settings_settings_screen_129),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SettingsDialogs(
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
private fun DeviceAndRuntimeCard(onClick: () -> Unit) {
    SettingsSurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(CoreUiR.string.settings_settings_screen_112),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(CoreUiR.string.settings_settings_screen_113),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(CoreUiR.string.settings_settings_screen_114),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    styled: Boolean = true,
    purpleTonal: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (styled) {
        AppSectionCard(
            title = title,
            tone = if (purpleTonal) AppSurfaceTone.TONAL else AppSurfaceTone.GLASS,
        ) {
            content()
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                content()
            }
        }
    }
}

@Composable
private fun SettingsSurfaceCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AppSurfaceCard(
        modifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier,
    ) {
        content()
    }
}

@Composable
private fun Toggle(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = label,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
    )
    Switch(
        checked = checked,
        onCheckedChange = onChanged,
        modifier = Modifier.scale(0.82f),
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
            checkedTrackColor = MaterialTheme.colorScheme.tertiary,
            checkedBorderColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
}

@Composable
private fun <T> EnumSelector(
    label: String,
    selected: T,
    values: Iterable<T>,
    text: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                val isSelected = value == selected
                OutlinedButton(
                    onClick = { onSelected(value) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                ) {
                    Text(text(value))
                }
            }
        }
    }
}

@Composable
private fun <T> EnumRadioGroup(
    label: String,
    selected: T,
    values: Iterable<T>,
    text: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        values.forEach { value ->
            val isSelected = value == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.RadioButton) { onSelected(value) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.tertiary,
                        unselectedColor = MaterialTheme.colorScheme.outline,
                    ),
                )
                Text(
                    text(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun StorageLine(label: String, bytes: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(bytes.readable())
    }
}

@Composable
private fun Long.readable(): String = when {
    this < 1_024 -> stringResource(CoreUiR.string.settings_storage_bytes, this)
    this < 1_048_576 -> stringResource(CoreUiR.string.settings_storage_kilobytes, this / 1_024)
    else -> stringResource(CoreUiR.string.settings_storage_megabytes, this / 1_048_576.0)
}

@Composable
private fun ThreadCountPolicy.label(): String = stringResource(
    when (this) {
        ThreadCountPolicy.ENGINE_DEFAULT -> CoreUiR.string.settings_thread_policy_engine_default
        ThreadCountPolicy.AVAILABLE_PROCESSORS -> CoreUiR.string.settings_thread_policy_available_processors
        ThreadCountPolicy.FIXED -> CoreUiR.string.settings_thread_policy_fixed
    },
)

@Composable
private fun ModelUnloadPolicy.label(): String = stringResource(
    when (this) {
        ModelUnloadPolicy.WHEN_IDLE -> CoreUiR.string.settings_model_unload_when_idle
        ModelUnloadPolicy.WHEN_BACKGROUND -> CoreUiR.string.settings_model_unload_when_background
        ModelUnloadPolicy.MANUAL -> CoreUiR.string.settings_model_unload_manual
    },
)

@Composable
private fun MetricDetail.label(): String = stringResource(
    when (this) {
        MetricDetail.STANDARD -> CoreUiR.string.settings_metric_detail_standard
        MetricDetail.VERBOSE -> CoreUiR.string.settings_metric_detail_verbose
    },
)
