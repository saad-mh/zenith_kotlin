package com.saadm.zenith.ui.people

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.saadm.zenith.ui.components.ZenithTextField
import com.saadm.zenith.ui.components.ActionSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

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

    ActionSheet(
        title = title,
        confirmLabel = "Save",
        enabled = name.isNotBlank(),
        onConfirm = { onSave(name.trim(), phone.trim().ifEmpty { null }, upiId.trim().ifEmpty { null }) },
        onDismiss = onDismiss
    ) {
        ZenithTextField(name, { name = it }, "Name")
        ZenithTextField(phone, { phone = it }, "Phone (optional)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        ZenithTextField(upiId, { upiId = it }, "UPI ID (optional)")
    }
}
