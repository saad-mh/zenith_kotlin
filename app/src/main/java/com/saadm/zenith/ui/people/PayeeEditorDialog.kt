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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Add or edit a person: a name, and optionally a phone number and UPI ID. */
@Composable
fun PayeeEditorDialog(
    title: String,
    initialName: String = "",
    initialPhone: String? = null,
    initialUpiId: String? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String?, upiId: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone.orEmpty()) }
    var upiId by rememberSaveable { mutableStateOf(initialUpiId.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = zenithColors().surfaceRaised,
        title = { Text(title, style = ZenithType.subheading, color = zenithColors().ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), phone.trim().ifEmpty { null }, upiId.trim().ifEmpty { null }) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
