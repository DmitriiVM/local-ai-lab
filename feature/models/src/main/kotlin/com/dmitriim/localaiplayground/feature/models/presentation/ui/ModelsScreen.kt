package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsUiState

@Composable
fun ModelsScreen(uiState: ModelsUiState) {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 20.dp,
                top = dimensions.topBarOverlayClearance,
                end = 20.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Models",
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (uiState.isEmpty) {
            StatusMessage(
                title = "No models installed",
                explanation = "Model import, validation, and lifecycle management arrive in Stage 2. " +
                    "The app never installs a model without your explicit action.",
            )
        }
    }
}
