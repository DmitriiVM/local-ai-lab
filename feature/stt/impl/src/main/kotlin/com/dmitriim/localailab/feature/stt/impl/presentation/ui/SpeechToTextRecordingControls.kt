package com.dmitriim.localailab.feature.stt.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.stt.impl.presentation.SttOperation

@Composable
internal fun RecordingControls(
    operation: SttOperation,
    hasInput: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onImport: () -> Unit,
    onRepeat: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    when (operation) {
        SttOperation.IDLE -> IdleRecordingControls(hasInput, onStart, onImport, onRepeat, onClear)
        SttOperation.RECORDING -> RecordingActiveControl(onStop)
        else -> RecordingBusyControls(operation, onCancel)
    }
}

@Composable
private fun RecordingActiveControl(onStop: () -> Unit) {
    Button(onClick = onStop) {
        Text(androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_139))
    }
}

@Composable
private fun RecordingBusyControls(
    operation: SttOperation,
    onCancel: () -> Unit,
) {
    when (operation) {
        SttOperation.STOPPING -> OutlinedButton(onClick = {}, enabled = false) {
            Text(androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_140))
        }
        SttOperation.IMPORTING -> {
            Column {
                OutlinedButton(onClick = {}, enabled = false) {
                    Text(androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_141))
                }
                TextButton(onClick = onCancel) {
                    Text(androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_142))
                }
            }
        }
        SttOperation.TRANSCRIBING -> {
            Column {
                OutlinedButton(onClick = {}, enabled = false) {
                    Text(androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_143))
                }
                TextButton(onClick = onCancel) {
                    Text(androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_144))
                }
            }
        }
        SttOperation.CANCELLING -> OutlinedButton(onClick = {}, enabled = false) {
            Text(androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_145))
        }
        SttOperation.IDLE,
        SttOperation.RECORDING,
        -> Unit
    }
}

@Composable
private fun IdleRecordingControls(
    hasInput: Boolean,
    onStart: () -> Unit,
    onImport: () -> Unit,
    onRepeat: () -> Unit,
    onClear: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_146),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onImport,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_147),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (hasInput) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onRepeat) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_148),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onClear) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(CoreUiR.string.stt_speech_to_text_screen_149),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

internal fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
