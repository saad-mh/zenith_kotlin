package com.saadm.zenith.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

enum class AmountTone { Neutral, Positive, Negative }

/**
 * Amount in paise, shown as ₹ with Indian grouping, a true minus, and muted cents.
 * The sign is always in the text, so the tone is never the only signal. TalkBack reads the spoken form.
 */
@Composable
fun AmountText(
    amount: Long,
    modifier: Modifier = Modifier,
    signed: Boolean = false,
    tone: AmountTone = AmountTone.Neutral,
    style: TextStyle = ZenithType.numeric
) {
    val colors = zenithColors()
    val money = MoneyFormatter.format(amount, signed)
    val color: Color = when (tone) {
        AmountTone.Neutral -> colors.ink
        AmountTone.Positive -> colors.positive
        AmountTone.Negative -> colors.negative
    }
    val text = buildAnnotatedString {
        append("${money.sign}₹${money.whole}")
        money.cents?.let { cents ->
            withStyle(SpanStyle(color = colors.inkMuted)) { append(".$cents") }
        }
    }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        modifier = modifier.semantics { contentDescription = money.spoken }
    )
}
