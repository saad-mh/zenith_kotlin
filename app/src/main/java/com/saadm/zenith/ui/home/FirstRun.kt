package com.saadm.zenith.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.transaction.TxnItems
import com.saadm.zenith.domain.transaction.TxnNames
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.components.EmptyState
import com.saadm.zenith.ui.components.TxnItemRow
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.ZenithTextButton
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

// Only ever drawn as a labelled example; it is never saved.
private val exampleEntry = TxnItems.of(
    listOf(
        TransactionEntity(
            id = 1, amount = 42_000, type = TxnType.EXPENSE, transactedAt = 0, createdAt = 0, updatedAt = 0,
            categoryId = 1, payeeId = null, accountId = null, note = "Third Wave Coffee", receiptUri = null
        )
    ),
    TxnNames(categories = mapOf(1L to "Food"), accounts = emptyMap())
).single()

/** Overview before anything is recorded: what an entry looks like, and where to start. */
@Composable
fun FirstRun(onAdd: () -> Unit, onAddAccount: () -> Unit) {
    EmptyState(
        title = "Start with one entry",
        body = "Log a spend, some income, or money a friend owes you. Zenith adds it up on this device, " +
            "so there is no bank link or sign-in to set up.",
        actionLabel = "Add your first transaction",
        onAction = onAdd,
        example = {
            ZenithCard(Modifier.fillMaxWidth()) {
                Text("Example · how an entry looks", style = ZenithType.caption, color = zenithColors().inkMuted)
                TxnItemRow(exampleEntry, caption = "Today")
            }
        }
    )
    ZenithTextButton("Add an account balance first", onAddAccount, Modifier.fillMaxWidth())
}

@Preview
@Composable
private fun FirstRunPreview() {
    PalettePreview { FirstRun({}, {}) }
}
