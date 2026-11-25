package com.saadm.zenith.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** The day above its entries: a small capital label, that day's total and a hairline. */
@Composable
fun DayHeader(label: String, total: Long?, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    Column(modifier.padding(top = ZenithSpacing.space3)) {
        Row {
            Text(
                label.uppercase(),
                style = ZenithType.caption,
                color = colors.inkMuted,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            total?.let { AmountText(it, signed = true, style = ZenithType.caption) }
        }
        HorizontalDivider(Modifier.padding(top = ZenithSpacing.space2), color = colors.border)
    }
}
