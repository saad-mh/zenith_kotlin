package com.saadm.zenith.ui.add

import com.saadm.zenith.ui.components.ZenithTextField
import com.saadm.zenith.ui.components.Lu
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
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
    val draftHint by viewModel.draftHint.collectAsState()
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

    val title = when {
        viewModel.isEdit -> "Edit transaction"
        viewModel.isRepeat -> "Repeat transaction"
        else -> "Add transaction"
    }
    // The amount step uses the whole height; the other steps are as tall as their content.
    ZenithBottomSheet(onDismissRequest, fill = step == Step.Amount) {
        if (step != Step.Amount) {
            Text(title, style = ZenithType.subheading, color = zenithColors().ink)
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
                title = title,
                onClose = onDismissRequest,
                draftHint = draftHint,
                form = form,
                categories = options.categoriesFor(form),
                defaultCategory = options.defaultFor(form),
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

@Composable
private fun KindSwitch(form: AddForm, onChange: ((AddForm) -> AddForm) -> Unit) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectionChips(form: AddForm, onChange: ((AddForm) -> AddForm) -> Unit) {
    if (form.kind.types.size <= 1) return
    val labels = if (form.kind == AddKind.Owed) listOf("They owe me", "I owe them") else listOf("I paid", "They paid")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
    ) {
        form.kind.types.forEachIndexed { index, type ->
            FilterChip(labels[index], form.type == type, { onChange { it.copy(type = type) } })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountStep(
    title: String,
    onClose: () -> Unit,
    draftHint: Boolean,
    form: AddForm,
    categories: List<CategoryEntity>,
    defaultCategory: Long?,
    payeeName: String?,
    accountName: String?,
    today: LocalDate,
    onChange: ((AddForm) -> AddForm) -> Unit,
    onDetails: () -> Unit,
    onSave: () -> Unit,
    onSplit: (() -> Unit)?
) {
    val colors = zenithColors()
    val gap = Arrangement.spacedBy(ZenithSpacing.space3)
    Column(verticalArrangement = gap) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp).background(colors.surfaceSunken, CircleShape)
            ) {
                Icon(painterResource(Lu.lucide_ic_x), contentDescription = "Close $title", tint = colors.ink, modifier = Modifier.size(20.dp))
            }
            Box(Modifier.weight(1f)) { KindSwitch(form, onChange) }
        }
        DirectionChips(form, onChange)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = gap, horizontalAlignment = Alignment.CenterHorizontally) {
        if (title != "Add transaction") Text(title, style = ZenithType.caption, color = colors.inkMuted)
        val shown = displayAmount(form.amount)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = colors.inkMuted, fontSize = ZenithType.heading.fontSize)) { append("₹ ") }
                append(shown)
            },
            style = ZenithType.display.copy(fontSize = 56.sp, lineHeight = 64.sp),
            color = if (form.amount.isEmpty()) colors.inkMuted else colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "$shown rupees"
                liveRegion = LiveRegionMode.Polite
            }
        )
        val sum = form.paise
        if (isSum(form.amount) && sum != null) {
            Text("= ${MoneyFormatter.format(sum).text}", style = ZenithType.bodyStrong, color = colors.ink)
        }
        FilterChip(form.text.ifBlank { "Add note" }, false, onDetails, icon = Lu.lucide_ic_text_align_start)
        if (form.amount.isNotEmpty() && draftHint) {
            Text("Closing keeps this as a draft", style = ZenithType.caption, color = colors.inkMuted)
        }
        // A zero amount needs no telling off before anything is typed.
        if (form.amount.isNotEmpty()) form.problem?.let { Text(it, style = ZenithType.caption, color = colors.inkMuted) }
    }

    Column(verticalArrangement = gap) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
        ) {
            CategoryChooser(categories, form.categoryId, defaultCategory) { id -> onChange { it.copy(categoryId = id, categoryPicked = true) } }
            FilterChip(dayLabel(form.date, today), false, onDetails, icon = Lu.lucide_ic_calendar)
            if (form.needsPayee) FilterChip(payeeName ?: "Choose person", false, onDetails, icon = Lu.lucide_ic_user)
            // A due moves no money, so it has no account.
            if (form.kind != AddKind.Owed) {
                FilterChip(accountName ?: "Paid with", false, onDetails, icon = Lu.lucide_ic_wallet)
            }
            if (onSplit != null && form.problem == null) FilterChip("Split", false, onSplit, icon = Lu.lucide_ic_split)
        }
        NumPad(
            onKey = { key -> onChange { it.copy(amount = typeKey(it.amount, key)) } },
            saveLabel = saveLabel(form),
            canSave = form.problem == null,
            onSave = onSave
        )
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
    ZenithTextField(form.text, onText, "Merchant or note")

    if (form.needsPayee) {
        Label("Person")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
        ) {
            options.payees.forEach { payee ->
                FilterChip(payee.name, form.payeeId == payee.id, { onChange { it.copy(payeeId = payee.id) } })
            }
        }
        NewPersonRow(onAddPayee)
    }

    val categories = options.categoriesFor(form)
    if (categories.isNotEmpty()) {
        Label("Category")
        CategoryChooser(categories, form.categoryId, options.defaultFor(form)) { id -> onChange { it.copy(categoryId = id, categoryPicked = true) } }
    }

    // A due moves no money, so it has no account.
    if (options.accounts.isNotEmpty() && form.kind != AddKind.Owed) {
        Label("Paid with")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
        ) {
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
            AmountStep("Add transaction", {}, true, AddForm(today, amount = "2450", categoryId = 1), categories, 1, null, "HDFC UPI", today, {}, {}, {}, {})
        }
    }
}
