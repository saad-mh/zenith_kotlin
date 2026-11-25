package com.saadm.zenith.ui.add

import com.saadm.zenith.ui.components.ZenithTextField
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.split.YOU
import com.saadm.zenith.ui.components.AmountField
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.AmountTone
import com.saadm.zenith.ui.components.Avatar
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.PrimaryButton
import com.saadm.zenith.ui.components.SegmentedControl
import com.saadm.zenith.ui.components.ZenithSwitch
import com.saadm.zenith.ui.components.ZenithTextButton
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** A field and button that creates a person and selects them. */
@Composable
fun NewPersonRow(onAdd: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ZenithTextField(name, { name = it }, "New person", Modifier.weight(1f))
        ZenithTextButton(
            "Add",
            {
                onAdd(name)
                name = ""
            },
            enabled = name.isNotBlank()
        )
    }
}

@Composable
fun WhoStep(
    form: AddForm,
    payees: List<Payee>,
    onChange: ((AddForm) -> AddForm) -> Unit,
    onAddPerson: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = zenithColors()
    ZenithTextButton("Back to amount", onBack)
    Text("Who's in this one?", style = ZenithType.subheading, color = colors.ink)
    payees.forEach { payee ->
        val chosen = payee.id in form.people
        ListRow(
            title = payee.name,
            leading = { Avatar(payee.name, Modifier.clearAndSetSemantics {}) },
            trailing = {
                FilterChip(
                    if (chosen) "In" else "Add",
                    chosen,
                    { onChange { it.copy(people = if (chosen) it.people - payee.id else it.people + payee.id) } },
                    Modifier.semantics { contentDescription = "Share with ${payee.name}" }
                )
            }
        )
    }
    NewPersonRow(onAddPerson)
    PrimaryButton("Next · Split", onNext, Modifier.fillMaxWidth(), enabled = form.people.isNotEmpty())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitStep(
    form: AddForm,
    payees: List<Payee>,
    onChange: ((AddForm) -> AddForm) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val colors = zenithColors()
    val total = form.paise ?: 0L
    val names = payees.associate { it.id to it.name }
    val people = form.people
    val shares = form.shares()
    val othersOwe = shares?.values?.sum() ?: 0L

    ZenithTextButton("Back to who", onBack)
    Text(MoneyFormatter.format(total).text, style = ZenithType.display, color = colors.ink)
    Text("You paid · split ${people.size + 1} ways", style = ZenithType.caption, color = colors.inkMuted)
    SegmentedControl(
        options = listOf("Equally", "By items", "Custom"),
        selectedIndex = form.splitMode.ordinal,
        onSelect = { index -> onChange { it.copy(splitMode = SplitMode.entries[index]) } }
    )

    if (form.splitMode == SplitMode.CUSTOM) {
        people.forEach { id ->
            AmountField(
                value = form.custom[id].orEmpty(),
                onValueChange = { typed -> onChange { it.copy(custom = it.custom + (id to typed)) } },
                label = "${names[id] ?: "Someone"} owes"
            )
        }
    }
    if (form.splitMode == SplitMode.BY_ITEMS) {
        form.items.forEachIndexed { index, item ->
            fun replace(next: DraftItem) = onChange { it.copy(items = it.items.toMutableList().also { list -> list[index] = next }) }
            AmountField(item.amount, { replace(item.copy(amount = it)) }, label = "Item ${index + 1}")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
                verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
            ) {
                (listOf(YOU) + people).forEach { id ->
                    val on = id in item.participants
                    FilterChip(
                        if (id == YOU) "You" else names[id] ?: "Someone",
                        on,
                        { replace(item.copy(participants = if (on) item.participants - id else item.participants + id)) }
                    )
                }
            }
            ZenithTextButton("Remove item ${index + 1}", { onChange { it.copy(items = it.items - item) } })
        }
        ZenithTextButton(
            "Add item",
            { onChange { it.copy(items = it.items + DraftItem(participants = (listOf(YOU) + it.people).toSet())) } }
        )
        Text("Anything not listed is shared equally.", style = ZenithType.caption, color = colors.inkMuted)
    }

    if (shares == null) {
        Text("These add up to more than the total.", style = ZenithType.bodyStrong, color = colors.negative)
    } else {
        ListRow(title = "You", subtitle = "Your share", trailing = { AmountText(total - othersOwe) })
        people.forEach { id ->
            val name = names[id] ?: "Someone"
            ListRow(
                title = name,
                subtitle = "Owes you",
                leading = { Avatar(name, Modifier.clearAndSetSemantics {}) },
                trailing = { AmountText(shares[id] ?: 0L) }
            )
        }
        ListRow(
            title = "Others owe you",
            trailing = { AmountText(othersOwe, signed = true, tone = AmountTone.Positive, style = ZenithType.heading) }
        )
        ZenithSwitch(
            "Share a payment request after saving",
            form.sendRequest,
            { on -> onChange { it.copy(sendRequest = on) } }
        )
    }
    PrimaryButton(
        "Save · others owe ${MoneyFormatter.format(othersOwe).text}",
        onSave,
        Modifier.fillMaxWidth(),
        enabled = othersOwe > 0
    )
}
