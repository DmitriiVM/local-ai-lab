package com.dmitriim.localaiplayground.feature.settings.presentation.ui

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

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
        title = { Text("Hugging Face access token") },
        text = {
            Column {
                Text("Enter a fine-grained read-only token. The token is encrypted and stored only on this device.")
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Access token") },
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
                Text(if (saving) "Saving…" else "Save token")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        },
    )
}
