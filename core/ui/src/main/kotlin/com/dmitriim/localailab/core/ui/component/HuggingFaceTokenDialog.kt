package com.dmitriim.localailab.core.ui.component

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.dmitriim.localailab.core.ui.R

@Composable
fun HuggingFaceTokenDialog(
    instruction: String,
    saveLabel: String,
    saving: Boolean,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var token by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.hugging_face_token_dialog_title)) },
        text = {
            Column {
                Text(instruction)
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.hugging_face_token_dialog_access_token)) },
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
                Text(if (saving) stringResource(R.string.hugging_face_token_dialog_saving) else saveLabel)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !saving) {
                Text(stringResource(R.string.hugging_face_token_dialog_cancel))
            }
        },
    )
}
