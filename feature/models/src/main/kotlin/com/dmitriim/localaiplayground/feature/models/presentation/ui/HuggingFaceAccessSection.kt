package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.dmitriim.localaiplayground.core.model.service.HuggingFaceCredentialStatus

@Composable
fun HuggingFaceAccessSection(
    accessUrl: String?,
    credentialStatus: HuggingFaceCredentialStatus,
    onConfigure: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    DetailsSection("Hugging Face access") {
        Text("This model requires that you accept its license in Hugging Face before downloading.")
        Text(huggingFaceCredentialStatusLabel(credentialStatus))
        accessUrl?.let { url ->
            OutlinedButton(onClick = { uriHandler.openUri(url) }) { Text("Open access page") }
        }
        OutlinedButton(onClick = onConfigure) {
            Text(if (credentialStatus == HuggingFaceCredentialStatus.USER_CONFIGURED) "Replace token" else "Configure token")
        }
    }
}

fun huggingFaceCredentialStatusLabel(status: HuggingFaceCredentialStatus): String = when (status) {
    HuggingFaceCredentialStatus.MISSING -> "No token configured"
    HuggingFaceCredentialStatus.USER_CONFIGURED -> "Token securely stored on this device"
    HuggingFaceCredentialStatus.DEBUG_CONFIGURED -> "Development token supplied by this debug build"
}
