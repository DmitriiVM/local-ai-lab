package com.dmitriim.localaiplayground.feature.playground.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.OperationState
import com.dmitriim.localaiplayground.feature.playground.presentation.PlaygroundUiState

@Composable
fun PlaygroundScreen(
    state: PlaygroundUiState,
    onRefresh: () -> Unit,
    onOpenCapability: (AiCapability) -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val visibleCapabilities = state.capabilities.filterNot {
        it.capability == AiCapability.VOICE_ACTIVITY_DETECTION ||
            it.capability == AiCapability.VOICE_ASSISTANT
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance,
            bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Local playgrounds", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Prompts, recordings, and generated content stay on this device. " +
                        "Network access is used only for model downloads you explicitly start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.operation is OperationState.Preparing) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Checking device engines…")
                }
            }
        }
        if (state.operation is OperationState.Error) {
            val error = state.operation.error
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(error.title, style = MaterialTheme.typography.titleMedium)
                        Text(error.explanation)
                        OutlinedButton(onClick = onRefresh) { Text("Retry") }
                    }
                }
            }
        }
        items(
            count = visibleCapabilities.size,
            key = { index -> visibleCapabilities[index].capability.name },
        ) { index ->
            val capability = visibleCapabilities[index]
            CapabilityCard(
                readiness = capability,
                onClick = { onOpenCapability(capability.capability) },
            )
        }
    }
}
