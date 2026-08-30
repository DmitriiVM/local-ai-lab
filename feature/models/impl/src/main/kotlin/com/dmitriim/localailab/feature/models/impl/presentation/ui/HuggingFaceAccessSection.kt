package com.dmitriim.localailab.feature.models.impl.presentation.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus

@Composable
fun HuggingFaceAccessSection(
    accessUrl: String?,
    credentialStatus: HuggingFaceCredentialStatus,
    onConfigure: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    DetailsSection(stringResource(CoreUiR.string.models_hugging_face_access)) {
        Text(stringResource(CoreUiR.string.models_hugging_face_access_section_37))
        Text(huggingFaceCredentialStatusLabel(credentialStatus))
        accessUrl?.let { url ->
            OutlinedButton(onClick = { uriHandler.openUri(url) }) { Text(stringResource(CoreUiR.string.models_hugging_face_access_section_38)) }
        }
        OutlinedButton(onClick = onConfigure) {
            Text(
                stringResource(
                    if (credentialStatus == HuggingFaceCredentialStatus.USER_CONFIGURED) {
                        CoreUiR.string.models_replace_token
                    } else {
                        CoreUiR.string.models_configure_token
                    },
                ),
            )
        }
    }
}

@Composable
fun huggingFaceCredentialStatusLabel(status: HuggingFaceCredentialStatus): String = stringResource(
    when (status) {
        HuggingFaceCredentialStatus.MISSING -> CoreUiR.string.models_hugging_face_no_token
        HuggingFaceCredentialStatus.USER_CONFIGURED -> CoreUiR.string.models_hugging_face_token_stored
        HuggingFaceCredentialStatus.DEBUG_CONFIGURED -> CoreUiR.string.models_hugging_face_debug_token
    },
)
