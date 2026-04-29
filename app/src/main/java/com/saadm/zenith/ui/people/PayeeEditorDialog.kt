package com.saadm.zenith.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

/**
 * Dialog for creating or editing a payee.
 * Allows users to enter name, phone, UPI ID, and optional avatar URI.
 */
@Composable
fun PayeeEditorDialog(
	title: String,
	initialName: String = "",
	initialPhone: String? = null,
	initialUpiId: String? = null,
	initialAvatarUri: String? = null,
	onDismiss: () -> Unit,
	onSave: (name: String, phone: String?, upiId: String?, avatarUri: String?) -> Unit
) {
	var name by remember(initialName) { mutableStateOf(initialName) }
	var phone by remember(initialPhone) { mutableStateOf(initialPhone ?: "") }
	var upiId by remember(initialUpiId) { mutableStateOf(initialUpiId ?: "") }
	var avatarUri by remember(initialAvatarUri) { mutableStateOf(initialAvatarUri ?: "") }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					label = { Text("Name") },
					singleLine = true,
					placeholder = { Text("e.g., Deez") }
				)

				OutlinedTextField(
					value = phone,
					onValueChange = { phone = it },
					label = { Text("Phone (optional)") },
					singleLine = true,
					placeholder = { Text("+91 11111 22222") }
				)

				OutlinedTextField(
					value = upiId,
					onValueChange = { upiId = it },
					label = { Text("UPI ID (optional)") },
					singleLine = true,
					placeholder = { Text("kyabaathai@hdfcbank") }
				)

				OutlinedTextField(
					value = avatarUri,
					onValueChange = { avatarUri = it },
					label = { Text("Avatar URI (optional)") },
					singleLine = true,
					placeholder = { Text("oops.com/img/1919") }
				)
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					if (name.isNotBlank()) {
						onSave(
							name.trim(),
							phone.ifBlank { null },
							upiId.ifBlank { null },
							avatarUri.ifBlank { null }
						)
					}
				}
			) {
				Text("Save")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancel") }
		}
	)
}

