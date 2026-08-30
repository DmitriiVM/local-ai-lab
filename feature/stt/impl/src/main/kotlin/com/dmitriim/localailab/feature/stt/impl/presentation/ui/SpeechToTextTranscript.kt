package com.dmitriim.localailab.feature.stt.impl.presentation.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSectionCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceTone

@Composable
internal fun SpeechToTextTranscript(
    transcript: String,
    clipboard: ClipboardManager,
    context: Context,
    onClear: () -> Unit,
) {
    if (transcript.isBlank()) return
    AppSectionCard("Final transcript", tone = AppSurfaceTone.TONAL) {
        Text(transcript, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.SansSerif)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(transcript)) }) {
                Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_136))
            }
            TextButton(onClick = { shareTranscript(context, transcript) }) {
                Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_137))
            }
            TextButton(
                onClick = onClear,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_138))
            }
        }
    }
}

private fun shareTranscript(context: Context, transcript: String) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, transcript)
            },
            "Share transcript",
        ),
    )
}
