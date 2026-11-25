package com.saadm.zenith.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
    ZenithTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        prefix = "₹",
        error = if (value.isNotBlank() && MoneyFormatter.parse(value) == null) {
            "Enter an amount above zero, with up to two decimals"
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
