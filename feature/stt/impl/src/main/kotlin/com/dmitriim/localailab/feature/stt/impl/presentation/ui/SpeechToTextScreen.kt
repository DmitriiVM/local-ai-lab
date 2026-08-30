package com.dmitriim.localailab.feature.stt.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.stt.impl.presentation.SpeechToTextUiState
import com.dmitriim.localailab.feature.stt.impl.presentation.SttLanguage
import com.dmitriim.localailab.feature.stt.impl.presentation.SttOperation

@Composable
fun SpeechToTextScreen(
    state: SpeechToTextUiState,
    onSelectModel: (ModelId) -> Unit,
    onSelectLanguage: (SttLanguage) -> Unit,
    onThreadCountChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onImportAudio: () -> Unit,
    onRepeat: () -> Unit,
    onProfile: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val busy = state.operation != SttOperation.IDLE
    val systemNavigationPadding = if (dimensions.bottomNavigationOverlayClearance == 0.dp) {
        Modifier.navigationBarsPadding()
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(systemNavigationPadding)
            .verticalScroll(rememberScrollState())
            .padding(
                start = dimensions.screenPadding,
                top = dimensions.topBarOverlayClearance + 50.dp,
                end = dimensions.screenPadding,
                bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        SpeechToTextSetup(
            state = state,
            busy = busy,
            onSelectModel = onSelectModel,
            onSelectLanguage = onSelectLanguage,
            onThreadCountChange = onThreadCountChange,
        )
        SpeechToTextAudioInput(
            state = state,
            busy = busy,
            onStart = onStartRecording,
            onStop = onStopRecording,
            onImport = onImportAudio,
            onRepeat = onRepeat,
            onProfile = onProfile,
            onCancel = onCancel,
            onClear = onClear,
        )
        SpeechToTextStatus(state)
        SpeechToTextTranscript(
            transcript = state.transcript,
            clipboard = clipboard,
            context = context,
            onClear = onClear,
        )
        state.metrics?.let { metrics ->
            SttRunMetricsCard(
                metrics = metrics,
                streamingModel = state.selectedModel?.recognitionMode ==
                    com.dmitriim.localailab.ai.api.model.manifest.SttRecognitionMode.STREAMING,
            )
        }
    }
}
