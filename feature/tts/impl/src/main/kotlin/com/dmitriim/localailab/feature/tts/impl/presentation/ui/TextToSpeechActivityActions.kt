package com.dmitriim.localailab.feature.tts.impl.presentation.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
internal fun rememberTextToSpeechActivityActions(
    callbacks: TextToSpeechActivityCallbacks,
): TextToSpeechActivityActions {
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/wav"),
    ) { uri ->
        uri?.let(callbacks.export)
    }
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            callbacks.startReferenceRecording()
        } else {
            callbacks.microphonePermissionDenied()
        }
    }
    val referencePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(callbacks.importReferenceAudio)
    }
    return TextToSpeechActivityActions(
        createExportDocument = exporter::launch,
        requestReferenceRecording = {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        },
        selectReferenceAudio = { referencePicker.launch(arrayOf("audio/*")) },
    )
}

internal class TextToSpeechActivityCallbacks(
    val export: (Uri) -> Unit,
    val startReferenceRecording: () -> Unit,
    val microphonePermissionDenied: () -> Unit,
    val importReferenceAudio: (Uri) -> Unit,
)

internal class TextToSpeechActivityActions(
    val createExportDocument: (String) -> Unit,
    val requestReferenceRecording: () -> Unit,
    val selectReferenceAudio: () -> Unit,
)
