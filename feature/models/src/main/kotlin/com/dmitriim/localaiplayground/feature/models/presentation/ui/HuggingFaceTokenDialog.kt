package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR

@Composable
fun HuggingFaceTokenDialog(
    saving: Boolean,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var token by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(CoreUiR.string.models_hugging_face_token_dialog_39)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(stringResource(CoreUiR.string.models_hugging_face_token_dialog_40))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(CoreUiR.string.models_hugging_face_token_dialog_41)) },
                    supportingText = error?.let { message -> { Text(message) } },
                    isError = error != null,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(token) }, enabled = token.isNotBlank() && !saving) {
                Text(stringResource(if (saving) CoreUiR.string.models_saving else CoreUiR.string.models_save_and_download))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(CoreUiR.string.models_hugging_face_token_dialog_42)) }
        },
    )
}
