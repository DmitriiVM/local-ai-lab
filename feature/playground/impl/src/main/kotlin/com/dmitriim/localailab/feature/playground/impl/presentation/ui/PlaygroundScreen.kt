package com.dmitriim.localailab.feature.playground.impl.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.playground.impl.R
import com.dmitriim.localailab.feature.playground.impl.presentation.PlaygroundUiState
import com.dmitriim.localailab.feature.playground.impl.presentation.state.DomainError
import com.dmitriim.localailab.feature.playground.impl.presentation.state.OperationState

@Composable
fun PlaygroundScreen(
    state: PlaygroundUiState,
    onRefresh: () -> Unit,
    onOpenCapability: (AiCapability) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current
    val visibleCapabilities = state.capabilities.filterNot {
        it.capability == AiCapability.VOICE_ACTIVITY_DETECTION ||
            it.capability == AiCapability.VOICE_ASSISTANT
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance,
            bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        item {
            PlaygroundHeader()
        }
        if (state.operation is OperationState.Preparing) {
            item { PlaygroundPreparingState() }
        }
        if (state.operation is OperationState.Error) {
            val error = state.operation.error
            item { PlaygroundErrorState(error = error, onRefresh = onRefresh) }
        }
        items(
            count = visibleCapabilities.size,
            key = { index -> visibleCapabilities[index].capability.name },
        ) { index ->
            val capability = visibleCapabilities[index]
            CapabilityCard(
                capability = capability.capability,
                onClick = { onOpenCapability(capability.capability) },
            )
        }
    }
}

@Composable
private fun PlaygroundHeader() {
    Column(
        modifier = Modifier.padding(top = 0.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_playground_brand_mark),
            contentDescription = null,
            modifier = Modifier
                .size(68.dp)
                .padding(bottom = 12.dp),
            colorFilter = ColorFilter.tint(AssistantPurple),
        )
        Text(
            text = stringResource(CoreUiR.string.playground_playground_screen_88),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(CoreUiR.string.playground_privacy_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun PlaygroundPreparingState() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(stringResource(CoreUiR.string.playground_playground_screen_90))
    }
}

@Composable
private fun PlaygroundErrorState(
    error: DomainError,
    onRefresh: () -> Unit,
) {
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
            OutlinedButton(onClick = onRefresh) {
                Text(stringResource(CoreUiR.string.playground_playground_screen_91))
            }
        }
    }
}

private val AssistantPurple = Color(0xFF3A236B)
