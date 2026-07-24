package com.dmitriim.localaiplayground.core.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusMessage(
    title: String,
    explanation: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun OperationStatus(
    state: OperationState<*>,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            OperationState.Idle -> Unit
            is OperationState.Preparing -> {
                CircularProgressIndicator()
                Text(state.message)
            }
            is OperationState.Running -> {
                if (state.completed != null && state.total != null && state.total > 0) {
                    LinearProgressIndicator(
                        progress = { state.completed.toFloat() / state.total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(state.message)
                if (onCancel != null) {
                    OutlinedButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            }
            is OperationState.Cancelling -> Text(state.message)
            is OperationState.Completed<*> -> Text("Completed")
            is OperationState.Error -> {
                StatusMessage(
                    title = state.error.title,
                    explanation = state.error.explanation,
                )
                if (state.error.retryable && onRetry != null) {
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
