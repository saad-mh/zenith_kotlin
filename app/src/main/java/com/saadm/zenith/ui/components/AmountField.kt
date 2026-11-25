package com.saadm.zenith.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.saadm.zenith.domain.model.MoneyFormatter

/** Rupee amount typed on the number keyboard. Read it with [MoneyFormatter.parse]. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        prefix = { Text("₹") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = value.isNotBlank() && MoneyFormatter.parse(value) == null,
        supportingText = if (value.isNotBlank() && MoneyFormatter.parse(value) == null) {
            { Text("Enter an amount above zero, with up to two decimals") }
        } else null
    )
}
