package com.saadm.zenith.ui.people

import com.saadm.zenith.ui.components.ActionSheet
import com.saadm.zenith.ui.common.LocalBottomBarInset
import com.saadm.zenith.ui.components.ZenithTextField
import com.saadm.zenith.ui.components.TotalAmount
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.payee.LedgerEntry
import com.saadm.zenith.domain.payee.PayeeLedger
import com.saadm.zenith.domain.payee.PayeeReminder
import com.saadm.zenith.domain.payee.balanceStatement
import com.saadm.zenith.domain.transaction.dayLabel
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.ScreenHeader
import com.saadm.zenith.ui.common.copyText
import com.saadm.zenith.ui.common.shareText
import com.saadm.zenith.ui.common.toLocalDate
import com.saadm.zenith.ui.components.AmountField
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.DateChips
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.PrimaryButton
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.ZenithBottomSheet
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.ZenithTextButton
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun PayeeDetailsScreen(onBack: () -> Unit, viewModel: PayeeDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var settling by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current
    val loaded = state

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(loaded?.payee?.name.orEmpty(), onBack) {
            if (loaded != null) ZenithTextButton("Edit", { editing = true })
        }
        if (loaded != null) {
            PayeeContent(
                state = loaded,
                onSettle = { settling = true },
                onRemind = {
                    PayeeReminder.textFor(loaded.payee.name, loaded.ledger.net)?.let(context::shareText)
                },
                onRemove = { viewModel.remove(onBack) },
                onStatement = { previewing = true },
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(ZenithSpacing.space4)
                    .padding(bottom = LocalBottomBarInset.current)
            )
        }
    }

    if (loaded == null) return
    if (settling) {
        SettlementSheet(
            state = loaded,
            onSave = { theyPaid, amount, date, accountId, note ->
                viewModel.settle(theyPaid, amount, date, accountId, note) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
                settling = false
            },
            onDismiss = { settling = false }
        )
    }
    if (previewing) {
        val text = balanceStatement(
            loaded.payee.name,
            loaded.ledger,
            loaded.transactions.mapValues { it.value.note }
        )
        // Shown in full first, so notes are never sent unseen.
        ActionSheet(
            title = "Statement",
            confirmLabel = "Share",
            dismissLabel = "Close",
            onConfirm = {
                previewing = false
                context.shareText(text)
            },
            onDismiss = { previewing = false }
        ) {
            ZenithCard { Text(text, style = ZenithType.body, color = zenithColors().ink) }
            SecondaryButton(
                "Copy",
                {
                    previewing = false
                    context.copyText(text)
                },
                Modifier.fillMaxWidth()
            )
        }
    }
    if (editing) {
        PayeeEditorDialog(
            title = "Edit person",
            initialName = loaded.payee.name,
            initialPhone = loaded.payee.phone,
            initialUpiId = loaded.payee.upiId,
            onDismiss = { editing = false },
            onSave = { name, phone, upiId ->
                viewModel.update(name, phone, upiId)
                editing = false
            }
        )
    }
}

private fun balanceWords(name: String, net: Long): String = when {
    net > 0 -> "$name owes you ${MoneyFormatter.format(net).text}"
    net < 0 -> "You owe $name ${MoneyFormatter.format(-net).text}"
    else -> "All settled with $name"
}

@Composable
private fun PayeeContent(
    state: PayeeDetailState,
    onSettle: () -> Unit,
    onRemind: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onStatement: () -> Unit = {}
) {
    val colors = zenithColors()
    val name = state.payee.name
    val net = state.ledger.net
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        ZenithCard {
            Text("Net balance", style = ZenithType.caption, color = colors.inkMuted)
            TotalAmount(net, signed = true)
            Text(balanceWords(name, net), style = ZenithType.bodyStrong, color = colors.ink)
        }
        PrimaryButton("Record settlement", onSettle, Modifier.fillMaxWidth())
        if (net > 0) SecondaryButton("Send a gentle reminder", onRemind, Modifier.fillMaxWidth())
        SecondaryButton("Share a statement", onStatement, Modifier.fillMaxWidth())

        Text("Shared history", style = ZenithType.subheading, color = colors.ink)
        Text(
            "+ adds to what $name owes you. − adds to what you owe, or pays them back.",
            style = ZenithType.caption,
            color = colors.inkMuted
        )
        if (state.ledger.history.isEmpty()) {
            Text("Nothing shared yet.", style = ZenithType.body, color = colors.inkMuted)
        } else {
            ZenithCard {
                // The ledger runs oldest first; show the newest at the top.
                state.ledger.history.asReversed().forEach { entry -> HistoryRow(state, entry) }
            }
        }
        if (net == 0L) {
            ZenithTextButton("Remove $name", onRemove, Modifier.fillMaxWidth())
        } else {
            Text("Settle the balance before removing $name.", style = ZenithType.caption, color = colors.inkMuted)
        }
    }
}

@Composable
private fun HistoryRow(state: PayeeDetailState, entry: LedgerEntry) {
    val name = state.payee.name
    val kind = when (entry.type) {
        TxnType.DUE_TO -> "$name owes you"
        TxnType.DUE_FROM -> "You owe $name"
        TxnType.SETTLEMENT_FROM -> "$name paid you"
        else -> "You paid $name"
    }
    val note = state.transactions[entry.transactionId]?.note?.takeIf { it.isNotBlank() }
    ListRow(
        title = note ?: kind,
        subtitle = listOfNotNull(
            dayLabel(entry.at.toLocalDate(), state.today),
            kind.takeIf { note != null }
        ).joinToString(" · "),
        trailing = {
            AmountText(if (addsToTheirDebt(entry.type)) entry.amount else -entry.amount, signed = true)
        },
        trailingCaption = "Balance ${MoneyFormatter.format(entry.runningBalance, signed = true).text}"
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettlementSheet(
    state: PayeeDetailState,
    onSave: (theyPaid: Boolean, amount: Long, date: LocalDate, accountId: Long?, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = zenithColors()
    val name = state.payee.name
    val net = state.ledger.net
    var theyPaid by rememberSaveable { mutableStateOf(net >= 0) }
    var amount by rememberSaveable { mutableStateOf(if (net == 0L) "" else MoneyFormatter.plain(abs(net))) }
    var date by remember { mutableStateOf(state.today) }
    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    val paise = MoneyFormatter.parse(amount)

    ZenithBottomSheet(onDismiss) {
        Text("Record settlement", style = ZenithType.subheading, color = colors.ink)
        Text("Saved on this device. Nothing is sent to $name.", style = ZenithType.caption, color = colors.inkMuted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
        ) {
            FilterChip("$name paid you", theyPaid, { theyPaid = true })
            FilterChip("You paid $name", !theyPaid, { theyPaid = false })
        }
        if (net != 0L) {
            FilterChip(
                "Full ${MoneyFormatter.format(abs(net)).text}",
                paise == abs(net),
                { amount = MoneyFormatter.plain(abs(net)) }
            )
        }
        AmountField(amount, { amount = it })
        if (state.accounts.isNotEmpty()) {
            Text("Method", style = ZenithType.bodyStrong, color = colors.ink)
            FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
        ) {
                state.accounts.forEach { account ->
                    val selected = accountId == account.id
                    FilterChip(account.name, selected, { accountId = if (selected) null else account.id })
                }
            }
        }
        DateChips(date, state.today) { date = it }
        ZenithTextField(note, { note = it }, "Note (optional)")
        if (paise != null) {
            Text(
                "After saving: ${balanceWords(name, balanceAfter(net, theyPaid, paise))}",
                style = ZenithType.bodyStrong,
                color = colors.ink
            )
        }
        PrimaryButton(
            "Save settlement",
            onClick = { paise?.let { onSave(theyPaid, it, date, accountId, note) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = paise != null
        )
    }
}

@Preview
@Composable
private fun PayeeDetailsPreview() {
    val due = TransactionEntity(
        id = 1, amount = 32_000, type = TxnType.DUE_TO, transactedAt = 0, createdAt = 0, updatedAt = 0,
        categoryId = 1, payeeId = 1, accountId = null, note = "Airport cab", receiptUri = null
    )
    val state = PayeeDetailState(
        today = LocalDate.of(2026, 10, 20),
        payee = Payee(1, "Dev", null, null, null, 0),
        ledger = PayeeLedger.of(listOf(due)),
        transactions = mapOf(1L to due),
        accounts = emptyList()
    )
    PalettePreview { PayeeContent(state, {}, {}, {}) }
}
