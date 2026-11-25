package com.saadm.zenith.ui.people

import com.saadm.zenith.ui.components.TotalAmount
import androidx.compose.runtime.key
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.payee.PayeeLedger
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.ScreenHeader
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.AmountTone
import com.saadm.zenith.ui.components.Avatar
import com.saadm.zenith.ui.components.EmptyState
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Shared balances. [createRequests] is bumped by the bottom bar's action, which opens the new-person dialog. */
@Composable
fun PeopleScreen(
    onNavigateToPayee: (Long) -> Unit,
    onBack: () -> Unit,
    createRequests: Int = 0,
    viewModel: PeopleViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    var filter by rememberSaveable { mutableStateOf(PayeeFilter.All) }
    var query by rememberSaveable { mutableStateOf("") }
    var creating by rememberSaveable { mutableStateOf(false) }
    var handledRequests by rememberSaveable { mutableIntStateOf(createRequests) }

    LaunchedEffect(createRequests) {
        if (createRequests > handledRequests) creating = true
        handledRequests = createRequests
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Shared balances", onBack)
        rows?.let {
            PeopleContent(
                rows = it,
                filter = filter,
                query = query,
                onFilter = { next -> filter = next },
                onQuery = { next -> query = next },
                onOpen = onNavigateToPayee,
                onAdd = { creating = true }
            )
        }
    }

    if (creating) {
        PayeeEditorDialog(
            title = "Add person",
            onDismiss = { creating = false },
            onSave = { name, phone, upiId ->
                viewModel.add(name, phone, upiId)
                creating = false
            }
        )
    }
}

@Composable
private fun PeopleContent(
    rows: List<PayeeRow>,
    filter: PayeeFilter,
    query: String,
    onFilter: (PayeeFilter) -> Unit,
    onQuery: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val sections = payeeSections(rows, filter, query)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(ZenithSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
    ) {
        if (rows.isEmpty()) {
            item {
                EmptyState(
                    title = "No shared balances yet",
                    body = "Split an expense, or record money you lent or borrowed. Each person's balance shows here.",
                    actionLabel = "Add a person",
                    onAction = onAdd,
                    example = {
                        ZenithCard {
                            Text("Example", style = ZenithType.caption, color = colors.inkMuted)
                            ListRow(
                                title = "Dev",
                                subtitle = "1 open item",
                                leading = { Avatar("Dev", Modifier.clearAndSetSemantics {}) },
                                trailing = { AmountText(32_000, signed = true, tone = AmountTone.Positive) },
                                trailingCaption = "owes you"
                            )
                        }
                    }
                )
            }
            return@LazyColumn
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
                ZenithCard {
                    Text("Net balance", style = ZenithType.caption, color = colors.inkMuted)
                    val net = rows.sumOf { it.ledger.net }
                    TotalAmount(net, signed = true)
                    Text(
                        when {
                            net > 0 -> "Owed to you, all together"
                            net < 0 -> "You owe, all together"
                            else -> "All square"
                        },
                        style = ZenithType.caption,
                        color = colors.inkMuted
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search people") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
                ) {
                    PayeeFilter.entries.forEach { FilterChip(it.label, filter == it, { onFilter(it) }) }
                }
            }
        }
        if (sections.isEmpty()) {
            item { EmptyState(title = "No results", body = "No one matches this search and filter.") }
        }
        items(sections, key = { it.title }) { section ->
            Column(Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)) {
                Row(
                    modifier = Modifier.padding(horizontal = ZenithSpacing.space1),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.title, style = ZenithType.bodyStrong, color = colors.ink, modifier = Modifier.weight(1f))
                    if (section.subtotal != 0L) AmountText(section.subtotal, signed = true, style = ZenithType.caption)
                }
                ZenithCard {
                    section.rows.forEach { row ->
                        key(row.payee.id) { PayeeListRow(row, onClick = { onOpen(row.payee.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayeeListRow(row: PayeeRow, onClick: () -> Unit) {
    val net = row.ledger.net
    val open = row.ledger.openDueCount
    ListRow(
        title = row.payee.name,
        subtitle = when {
            net == 0L -> "Settled"
            open == 1 -> "1 open item"
            else -> "$open open items"
        },
        leading = { Avatar(row.payee.name, Modifier.clearAndSetSemantics {}) },
        trailing = {
            AmountText(
                net,
                signed = true,
                tone = when {
                    net > 0 -> AmountTone.Positive
                    net < 0 -> AmountTone.Negative
                    else -> AmountTone.Neutral
                }
            )
        },
        trailingCaption = when {
            net > 0 -> "owes you"
            net < 0 -> "you owe"
            else -> null
        },
        onClick = onClick
    )
}

@Preview
@Composable
private fun PeoplePreview() {
    val rows = listOf(
        PayeeRow(Payee(1, "Dev", null, null, null, 0), PayeeLedger.of(emptyList()))
    )
    PalettePreview {
        PeopleContent(rows, PayeeFilter.All, "", {}, {}, {}, {}, Modifier.height(320.dp))
        PeopleContent(emptyList(), PayeeFilter.All, "", {}, {}, {}, {}, Modifier.height(240.dp))
    }
}
