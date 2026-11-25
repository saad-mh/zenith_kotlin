package com.saadm.zenith.ui.categories

import com.saadm.zenith.ui.common.LocalBottomBarInset
import androidx.compose.ui.res.painterResource
import com.saadm.zenith.ui.components.ZenithTextField
import com.saadm.zenith.ui.components.ActionSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.ScreenHeader
import com.saadm.zenith.ui.common.shortMonthName
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.CategoryBadge
import com.saadm.zenith.ui.components.categoryIcons
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.MonthSwitcher
import com.saadm.zenith.ui.components.PrimaryButton
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.SegmentedControl
import com.saadm.zenith.ui.components.ZenithBottomSheet
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate

private val types = listOf(TxnType.EXPENSE, TxnType.INCOME)

@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var openId by remember { mutableStateOf<Long?>(null) }
    var adding by remember { mutableStateOf(false) }
    val loaded = state

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Categories", onBack) {
            if (loaded != null) {
                MonthSwitcher(
                    label = "${loaded.period.startDate.shortMonthName()} ${loaded.period.startDate.year}",
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    nextEnabled = !loaded.isCurrentMonth
                )
            }
        }
        if (loaded != null) {
            CategoriesContent(
                state = loaded,
                onType = viewModel::show,
                onOpen = { openId = it },
                onAdd = { adding = true },
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(ZenithSpacing.space4)
                    .padding(bottom = LocalBottomBarInset.current)
            )
        }
    }

    if (loaded == null) return
    val all = loaded.rows.map { it.category }
    if (adding) {
        CategorySheet(
            category = null,
            others = all,
            onSave = { name, icon ->
                viewModel.add(name, icon)
                adding = false
            },
            onDismiss = { adding = false }
        )
    }
    all.firstOrNull { it.id == openId }?.let { category ->
        CategorySheet(
            category = category,
            others = all,
            onSave = { name, icon ->
                viewModel.edit(category, name, icon)
                openId = null
            },
            onDismiss = { openId = null },
            onArchive = {
                viewModel.archive(category)
                openId = null
            },
            onMerge = {
                viewModel.merge(category, it)
                openId = null
            }
        )
    }
}

@Composable
private fun CategoriesContent(
    state: CategoriesState,
    onType: (TxnType) -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        SegmentedControl(listOf("Spending", "Income"), types.indexOf(state.type), { onType(types[it]) })
        ZenithCard {
            state.rows.forEach { row ->
                ListRow(
                    title = row.category.name,
                    subtitle = if (row.category.isDefault) "Default" else null,
                    leading = { CategoryBadge(row.category.emoji, row.category.name) },
                    trailing = { AmountText(row.total) },
                    onClick = { onOpen(row.category.id) }
                )
            }
        }
        PrimaryButton("Add category", onAdd, Modifier.fillMaxWidth())
    }
}

/** Add, or rename, merge and archive. [category] null means a new one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySheet(
    category: CategoryEntity?,
    others: List<CategoryEntity>,
    onSave: (name: String, icon: String) -> Unit,
    onDismiss: () -> Unit,
    onArchive: () -> Unit = {},
    onMerge: (CategoryEntity) -> Unit = {}
) {
    val colors = zenithColors()
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    // Starts as whatever is stored, so saving without picking never loses an unknown key.
    var icon by remember(category?.id) { mutableStateOf(category?.emoji.orEmpty()) }
    var mergeInto by remember(category?.id) { mutableStateOf<CategoryEntity?>(null) }
    var confirming by remember(category?.id) { mutableStateOf(false) }
    val problem = nameProblem(name, others, category?.id)

    ZenithBottomSheet(onDismiss) {
        Text(if (category == null) "Add category" else category.name, style = ZenithType.subheading, color = colors.ink)
        ZenithTextField(name, { name = it }, "Name", error = problem.takeIf { name.isNotEmpty() })
        Text("Icon", style = ZenithType.bodyStrong, color = colors.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space1)) {
            categoryIcons.forEach { choice ->
                IconToggleButton(checked = icon == choice.key, onCheckedChange = { icon = choice.key }) {
                    Icon(
                        painterResource(choice.image),
                        contentDescription = choice.label,
                        tint = if (icon == choice.key) colors.onStrong else colors.ink,
                        modifier = Modifier
                            .background(if (icon == choice.key) colors.primaryStrong else colors.surfaceSunken, CircleShape)
                            .padding(ZenithSpacing.space2)
                    )
                }
            }
        }
        PrimaryButton("Save", { onSave(name, icon) }, Modifier.fillMaxWidth(), enabled = problem == null)

        if (category == null) return@ZenithBottomSheet
        if (category.isDefault) {
            Text("The default category can't be merged or archived.", style = ZenithType.caption, color = colors.inkMuted)
            return@ZenithBottomSheet
        }
        val targets = others.filter { it.id != category.id }
        if (targets.isNotEmpty()) {
            Text("Merge into", style = ZenithType.bodyStrong, color = colors.ink)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
                verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
            ) {
                targets.forEach { target ->
                    FilterChip(target.name, mergeInto?.id == target.id, { mergeInto = target })
                }
            }
            SecondaryButton("Merge", { confirming = true }, Modifier.fillMaxWidth(), enabled = mergeInto != null)
        }
        SecondaryButton("Archive", onArchive, Modifier.fillMaxWidth())
    }

    val target = mergeInto
    if (confirming && category != null && target != null) {
        ActionSheet(
            title = "Merge ${category.name} into ${target.name}?",
            message = "Every entry and budget in ${category.name} moves to ${target.name}. Where both have a budget " +
                "for a month, ${target.name}'s is kept. This cannot be undone.",
            confirmLabel = "Merge",
            destructive = true,
            onConfirm = { onMerge(target) },
            onDismiss = { confirming = false }
        )
    }
}

@Preview
@Composable
private fun CategoriesPreview() {
    val period = MonthPeriods.containing(LocalDate.of(2026, 10, 20))
    val rows = listOf("Food", "Transport").mapIndexed { i, name ->
        CategoryRow(CategoryEntity(i + 1L, name, "", "#000000", TxnType.EXPENSE, i), 684_000L / (i + 1))
    }
    PalettePreview { CategoriesContent(CategoriesState(period, true, TxnType.EXPENSE, rows), {}, {}, {}) }
}
