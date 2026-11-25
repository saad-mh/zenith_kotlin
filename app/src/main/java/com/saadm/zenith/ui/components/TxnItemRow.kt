package com.saadm.zenith.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saadm.zenith.domain.transaction.TxnItem

/** A transaction in a list: who or what, a word for its kind, the signed amount and an optional date. */
@Composable
fun TxnItemRow(item: TxnItem, modifier: Modifier = Modifier, caption: String? = null, onClick: (() -> Unit)? = null) {
    ListRow(
        title = item.title,
        modifier = modifier,
        subtitle = listOfNotNull(item.tag?.word, item.detail.ifEmpty { null }).joinToString(" · "),
        leading = {
            if (item.people.size > 1) AvatarStack(item.people, maxVisible = 2)
            else Avatar(item.people.firstOrNull() ?: item.title)
        },
        trailing = {
            AmountText(
                amount = item.signedAmount,
                signed = true,
                tone = if (item.signedAmount > 0) AmountTone.Positive else AmountTone.Neutral
            )
        },
        trailingCaption = caption,
        onClick = onClick
    )
}
