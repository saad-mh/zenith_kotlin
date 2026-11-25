package com.saadm.zenith.ui.add

import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.transaction.dayLabel
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.shareText
import com.saadm.zenith.ui.components.DateChips
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.PrimaryButton
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.SegmentedControl
import com.saadm.zenith.ui.components.ZenithBottomSheet
import com.saadm.zenith.ui.components.ZenithTextButton
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate

private enum class Step { Amount, Details, Who, Split }

/** Add or edit an entry: the amount first, then the details. */
@Composable
fun AddTransactionFlow(onDismissRequest: () -> Unit, viewModel: AddTxnViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsState()
    val options by viewModel.options.collectAsState()
    val saved by viewModel.saved.collectAsState()
    var step by rememberSaveable { mutableStateOf(Step.Amount) }
    val context = LocalContext.current

    val view = LocalView.current
    LaunchedEffect(saved) {
        val done = saved ?: return@LaunchedEffect
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        done.request?.let(context::shareText)
        onDismissRequest()
    }
    // Only a new expense can be split.
    val canSplit = !viewModel.isEdit && form.type == TxnType.EXPENSE
    val startSplit = { step = Step.Who }

    ZenithBottomSheet(onDismissRequest) {
        Text(
            when {
                viewModel.isEdit -> "Edit transaction"
                viewModel.isRepeat -> "Repeat transaction"
                else -> "Add transaction"
            },
            style = ZenithType.subheading,
            color = zenithColors().ink
        )
        if (viewModel.keepsDraft) {
            Text("Closing keeps this as a draft", style = ZenithType.caption, color = zenithColors().inkMuted)
        }
        when (step) {
            Step.Details -> DetailsStep(
                form = form,
                options = options,
                today = viewModel.today,
                onChange = viewModel::change,
                onText = viewModel::setText,
                onAddPayee = { name -> viewModel.addPayee(name) { f, id -> f.copy(payeeId = id) } },
                onBack = { step = Step.Amount },
                onSave = viewModel::save,
                onSplit = if (canSplit) startSplit else null
            )
            Step.Amount -> AmountStep(
                form = form,
                categories = options.categoriesFor(form),
                payeeName = options.payees.firstOrNull { it.id == form.payeeId }?.name,
                accountName = options.accounts.firstOrNull { it.id == form.accountId }?.name,
                today = viewModel.today,
                onChange = viewModel::change,
                onDetails = { step = Step.Details },
                onSave = viewModel::save,
                onSplit = if (canSplit) startSplit else null
            )
            Step.Who -> WhoStep(
                form = form,
                payees = options.payees,
                onChange = viewModel::change,
                onAddPerson = { name -> viewModel.addPayee(name) { f, id -> f.copy(people = f.people + id) } },
                onBack = { step = Step.Amount },
                onNext = { step = Step.Split }
            )
            Step.Split -> SplitStep(
                form = form,
                payees = options.payees,
                onChange = viewModel::change,
                onBack = { step = Step.Who },
                onSave = viewModel::saveSplit
            )
        }
    }
}

private fun saveLabel(form: AddForm): String {
    val what = when (form.type) {
        TxnType.EXPENSE -> "expense"
        TxnType.INCOME -> "income"
        TxnType.DUE_TO, TxnType.DUE_FROM -> "due"
        else -> "payment"
    }
    return "Save $what" + (form.paise?.let { " · ${MoneyFormatter.format(it).text}" } ?: "")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeSwitch(form: AddForm, onChange: ((AddForm) -> AddForm) -> Unit) {
    val kinds = AddKind.entries
    SegmentedControl(
        options = kinds.map { it.label },
        selectedIndex = kinds.indexOf(form.kind),
        onSelect = { index ->
            val kind = kinds[index]
            if (kind != form.kind) {
                onChange { it.copy(type = kind.types.first(), categoryId = null, categoryPicked = false) }
            }
        }
    )
    if (form.kind.types.size > 1) {
        val labels = if (form.kind == AddKind.Owed) listOf("They owe me", "I owe them") else listOf("I paid", "They paid")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            form.kind.types.forEachIndexed { index, type ->
                FilterChip(labels[index], form.type == type, { onChange { it.copy(type = type) } })
            }
        }
    }
}

@Composable
private fun AmountStep(
    form: AddForm,
    categories: List<CategoryEntity>,
    payeeName: String?,
    accountName: String?,
    today: LocalDate,
    onChange: ((AddForm) -> AddForm) -> Unit,
    onDetails: () -> Unit,
    onSave: () -> Unit,
    onSplit: (() -> Unit)?
) {
    val colors = zenithColors()
    TypeSwitch(form, onChange)
    val shown = displayAmount(form.amount)
    Text(
        "₹$shown",
        style = ZenithType.display,
        color = if (form.amount.isEmpty()) colors.inkMuted else colors.ink,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "$shown rupees"
            liveRegion = LiveRegionMode.Polite
        }
    )
    val sum = form.paise
    if (isSum(form.amount) && sum != null) {
        Text(
            "= ${MoneyFormatter.format(sum).text}",
            style = ZenithType.bodyStrong,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (categories.isNotEmpty()) {
        CategoryChips(form, categories, onChange, Modifier.horizontalScroll(rememberScrollState()))
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
    ) {
        SecondaryButton(dayLabel(form.date, today), onDetails)
        if (form.needsPayee) SecondaryButton(payeeName ?: "Choose person", onDetails)
        SecondaryButton(accountName ?: "Paid with", onDetails)
        SecondaryButton(form.text.ifBlank { "Add note" }, onDetails)
    }
    NumPad(onKey = { key -> onChange { it.copy(amount = typeKey(it.amount, key)) } })
    form.problem?.let { Text(it, style = ZenithType.caption, color = colors.inkMuted) }
    if (onSplit != null) {
        PrimaryButton("Next · Who's in?", onSplit, Modifier.fillMaxWidth(), enabled = form.problem == null)
        SecondaryButton("Save as personal", onSave, Modifier.fillMaxWidth(), enabled = form.problem == null)
    } else {
        PrimaryButton(saveLabel(form), onSave, Modifier.fillMaxWidth(), enabled = form.problem == null)
    }
    ZenithTextButton("More details", onDetails, Modifier.fillMaxWidth())
}

@Composable
private fun CategoryChips(
    form: AddForm,
    categories: List<CategoryEntity>,
    onChange: ((AddForm) -> AddForm) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        categories.forEach { category ->
            FilterChip(
                category.name,
                form.categoryId == category.id,
                { onChange { it.copy(categoryId = category.id, categoryPicked = true) } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsStep(
    form: AddForm,
    options: AddOptions,
    today: LocalDate,
    onChange: ((AddForm) -> AddForm) -> Unit,
    onText: (String) -> Unit,
    onAddPayee: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSplit: (() -> Unit)?
) {
    val colors = zenithColors()
    ZenithTextButton("Back to amount", onBack)
    OutlinedTextField(
        value = form.text,
        onValueChange = onText,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Merchant or note") },
        singleLine = true
    )

    if (form.needsPayee) {
        Label("Person")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            options.payees.forEach { payee ->
                FilterChip(payee.name, form.payeeId == payee.id, { onChange { it.copy(payeeId = payee.id) } })
            }
        }
        NewPersonRow(onAddPayee)
    }

    val categories = options.categoriesFor(form)
    if (categories.isNotEmpty()) {
        Label("Category")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            categories.forEach { category ->
                FilterChip(
                    category.name,
                    form.categoryId == category.id,
                    { onChange { it.copy(categoryId = category.id, categoryPicked = true) } }
                )
            }
        }
    }

    // A due moves no money, so it has no account.
    if (options.accounts.isNotEmpty() && form.kind != AddKind.Owed) {
        Label("Paid with")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            options.accounts.forEach { account ->
                val selected = form.accountId == account.id
                FilterChip(
                    account.name,
                    selected,
                    { onChange { it.copy(accountId = if (selected) null else account.id) } }
                )
            }
        }
    }

    Label("Date")
    DateChips(form.date, today) { date -> onChange { it.copy(date = date) } }

    form.problem?.let { Text(it, style = ZenithType.caption, color = colors.inkMuted) }
    if (onSplit != null) {
        SecondaryButton("Split with others", onSplit, Modifier.fillMaxWidth(), enabled = form.problem == null)
    }
    PrimaryButton(saveLabel(form), onSave, Modifier.fillMaxWidth(), enabled = form.problem == null)

}

@Composable
private fun Label(text: String) {
    Text(text, style = ZenithType.bodyStrong, color = zenithColors().ink)
}

@Preview
@Composable
private fun AmountStepPreview() {
    val today = LocalDate.of(2026, 10, 20)
    val categories = listOf(CategoryEntity(1, "Food", "", "#000000", TxnType.EXPENSE, 0))
    PalettePreview {
        Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)) {
            AmountStep(AddForm(today, amount = "2450", categoryId = 1), categories, null, "HDFC UPI", today, {}, {}, {}, {})
        }
    }
}
