package com.dmitriim.localaiplayground.feature.runs.presentation.ui

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

@Composable
fun RunsScreen() {
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
            text = "Runs",
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        StatusMessage(
            title = "No runs yet",
            explanation = "Completed and cancelled local inference runs will appear here once " +
                "playground vertical slices are available.",
        )
    }
}
