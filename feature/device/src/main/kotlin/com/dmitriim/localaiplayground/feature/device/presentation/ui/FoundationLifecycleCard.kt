package com.dmitriim.localaiplayground.feature.device.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.OperationState
import com.dmitriim.localaiplayground.core.result.OperationStatus

@Composable
internal fun FoundationLifecycleCard(
    operation: OperationState<Unit>,
    onRun: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Foundation lifecycle check", style = MaterialTheme.typography.titleMedium)
            Text(
                "This clearly named placeholder verifies that foreground-only work is " +
                    "cancelled safely when the app is backgrounded.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OperationStatus(state = operation, onCancel = onCancel, onRetry = onRun)
            if (operation is OperationState.Idle || operation is OperationState.Completed) {
                OutlinedButton(onClick = onRun) { Text("Run 5-second check") }
            }
        }
    }
}
