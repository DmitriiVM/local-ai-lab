package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.assistant.presentation.AssistantInputMode
import androidx.compose.ui.res.stringResource
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AssistantInputModeSettingsSheet(
    selectedMode: AssistantInputMode,
    enabled: Boolean,
    onSelectMode: (AssistantInputMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(CoreUiR.string.assistant_assistant_input_mode_settings_sheet_10), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(CoreUiR.string.assistant_assistant_input_mode_settings_sheet_11),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistantInputModeOption(
                mode = AssistantInputMode.DICTATE,
                selected = selectedMode == AssistantInputMode.DICTATE,
                enabled = enabled,
                onSelect = { mode ->
                    onSelectMode(mode)
                    onDismiss()
                },
            )
            HorizontalDivider()
            AssistantInputModeOption(
                mode = AssistantInputMode.VOICE,
                selected = selectedMode == AssistantInputMode.VOICE,
                enabled = enabled,
                onSelect = { mode ->
                    onSelectMode(mode)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun AssistantInputModeOption(
    mode: AssistantInputMode,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (AssistantInputMode) -> Unit,
) {
    val title = if (mode == AssistantInputMode.DICTATE) "Dictate" else "Voice"
    val description = if (mode == AssistantInputMode.DICTATE) {
        "Add the transcript to your editable draft. Review it, then send when ready."
    } else {
        "Send the final transcript automatically and speak the completed response."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.RadioButton) { onSelect(mode) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
