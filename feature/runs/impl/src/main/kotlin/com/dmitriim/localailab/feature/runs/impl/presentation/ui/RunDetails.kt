package com.dmitriim.localailab.feature.runs.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord

@Composable
internal fun RunDetails(
    run: RunRecord,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onRepeat: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance + 20.dp,
            bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        item {
            RunDetailsActions(
                onClose = onClose,
                onRepeat = onRepeat,
                onShare = onShare,
            )
        }
        item { RunDetailsSummary(run) }
        item { RunDetailsStatus(run) }
        item { RunDetailsCards(run) }
    }
}

@Composable
private fun RunDetailsActions(
    onClose: () -> Unit,
    onRepeat: () -> Unit,
    onShare: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onClose) {
            Text(stringResource(CoreUiR.string.runs_runs_screen_100))
        }
        OutlinedButton(onClick = onRepeat) {
            Text(stringResource(CoreUiR.string.runs_runs_screen_101))
        }
        OutlinedButton(onClick = onShare) {
            Text(stringResource(CoreUiR.string.runs_runs_screen_102))
        }
    }
}

@Composable
private fun RunDetailsSummary(run: RunRecord) {
    Text(
        text = stringResource(
            CoreUiR.string.runs_kind_and_capability,
            run.kind.label(),
            run.capability.label(),
        ),
        style = MaterialTheme.typography.headlineMedium,
    )
}

@Composable
private fun RunDetailsStatus(run: RunRecord) {
    Text(
        text = stringResource(
            CoreUiR.string.runs_status_and_model,
            run.status.label(),
            run.model?.displayName ?: stringResource(CoreUiR.string.runs_model_removed),
        ),
    )
}

@Composable
private fun RunDetailsCards(run: RunRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailCard(
            title = stringResource(CoreUiR.string.runs_input),
            body = run.input ?: stringResource(CoreUiR.string.runs_not_retained),
        )
        DetailCard(
            title = stringResource(CoreUiR.string.runs_output),
            body = run.output ?: stringResource(CoreUiR.string.runs_not_available),
        )
        DetailCard(
            title = stringResource(CoreUiR.string.runs_effective_parameters),
            body = run.parametersJson,
        )
        DetailCard(
            title = stringResource(CoreUiR.string.runs_metrics),
            body = run.metricsJson,
        )
        run.errorMessage?.let { error ->
            DetailCard(
                title = stringResource(CoreUiR.string.runs_error),
                body = error,
            )
        }
        if (run.linkedRunIds.isNotEmpty()) {
            DetailCard(
                title = stringResource(CoreUiR.string.runs_linked_pipeline_runs),
                body = run.linkedRunIds.joinToString(),
            )
        }
        Text(
            text = stringResource(CoreUiR.string.runs_export_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
