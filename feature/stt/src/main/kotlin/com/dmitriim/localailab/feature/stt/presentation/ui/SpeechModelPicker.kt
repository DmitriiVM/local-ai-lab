package com.dmitriim.localailab.feature.stt.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.InstalledOptionList
import com.dmitriim.localailab.core.ui.component.OptionPickerItem
import com.dmitriim.localailab.feature.stt.presentation.SpeechModelOption

@Composable
internal fun SpeechModelPicker(
    models: List<SpeechModelOption>,
    selectedId: ModelId?,
    enabled: Boolean,
    onSelect: (ModelId) -> Unit,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val selected = models.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(CoreUiR.string.stt_speech_model_picker_130), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { sheetVisible = true },
            enabled = enabled && models.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selected?.displayName ?: stringResource(CoreUiR.string.stt_install_speech_model),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (sheetVisible) {
        SpeechModelSelectionSheet(
            models = models,
            selectedId = selectedId,
            enabled = enabled,
            onSelect = onSelect,
            onDismiss = { sheetVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeechModelSelectionSheet(
    models: List<SpeechModelOption>,
    selectedId: ModelId?,
    enabled: Boolean,
    onSelect: (ModelId) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = models.map(SpeechModelOption::toPickerItem)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(CoreUiR.string.ui_copy_66),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = stringResource(CoreUiR.string.ui_copy_67),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 12.dp),
            )
            InstalledOptionList(
                items = items,
                selectedId = selectedId?.value,
                enabled = enabled,
                onSelect = { id ->
                    models.firstOrNull { it.id.value == id }?.let { model -> onSelect(model.id) }
                    onDismiss()
                },
                noInstalledItemsLabel = "No speech models are installed.",
            )
        }
    }
}

private fun SpeechModelOption.toPickerItem() = OptionPickerItem(
    id = id.value,
    label = displayName,
    supportingText = languages.joinToString(),
    installed = installed,
)
