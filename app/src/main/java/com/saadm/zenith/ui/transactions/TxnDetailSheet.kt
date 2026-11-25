package com.saadm.zenith.ui.transactions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.transaction.TxnItem
import com.saadm.zenith.domain.transaction.TxnNames
import com.saadm.zenith.domain.transaction.dayLabel
import com.saadm.zenith.ui.common.toLocalDate
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.ZenithBottomSheet
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate

@Composable
fun TxnDetailSheet(
    item: TxnItem,
    names: TxnNames,
    today: LocalDate,
    onEdit: () -> Unit,
    onRepeat: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = zenithColors()
    var confirming by remember { mutableStateOf(false) }
    ZenithBottomSheet(onDismiss) {
        Text(item.title, style = ZenithType.subheading, color = colors.ink)
        AmountText(item.signedAmount, signed = true, style = ZenithType.display)
        item.tag?.let { Text(it.word, style = ZenithType.bodyStrong, color = colors.ink) }
        Detail("Date", dayLabel(item.txn.transactedAt.toLocalDate(), today))
        Detail("Category", names.categories[item.txn.categoryId] ?: "Uncategorized")
        names.accounts[item.txn.accountId]?.let { Detail("Paid with", it) }
        item.txn.note?.takeIf { it.isNotBlank() }?.let { Detail("Note", it) }
        if (item.shares.isNotEmpty()) {
            Text("Split", style = ZenithType.bodyStrong, color = colors.ink)
            Share("Your share", item.yourShare)
            item.shares.forEach { share ->
                Share("${names.payees[share.payeeId] ?: "Someone"} owes you", share.amount)
            }
        }
        // Transfers are two linked rows and splits carry shares, so neither is edited here.
        val transfer = item.txn.type == TxnType.TRANSFER_IN || item.txn.type == TxnType.TRANSFER_OUT
        if (!transfer && item.shares.isEmpty()) {
            SecondaryButton("Edit", onEdit, Modifier.fillMaxWidth())
            SecondaryButton("Repeat", onRepeat, Modifier.fillMaxWidth())
        }
        SecondaryButton("Delete", { confirming = true }, Modifier.fillMaxWidth())
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Delete this entry?") },
            text = {
                Text(
                    if (item.shares.isEmpty()) "It is removed for good."
                    else "The expense and what others owe for it are removed for good."
                )
            },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = colors.negative) } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Keep it") } }
        )
    }
}

@Composable
private fun Detail(label: String, value: String) {
    val colors = zenithColors()
    Row {
        Text(label, style = ZenithType.body, color = colors.inkMuted, modifier = Modifier.weight(1f))
        Text(value, style = ZenithType.bodyStrong, color = colors.ink, modifier = Modifier.weight(2f))
    }
}

@Composable
private fun Share(label: String, amount: Long) {
    Row {
        Text(label, style = ZenithType.body, color = zenithColors().ink, modifier = Modifier.weight(1f))
        AmountText(amount)
    }
}
