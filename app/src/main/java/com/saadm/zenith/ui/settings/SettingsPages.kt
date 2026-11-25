package com.saadm.zenith.ui.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.MonthStartDay
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.model.WeekStart
import com.saadm.zenith.ui.common.LocalBottomBarInset
import com.saadm.zenith.ui.common.ScreenHeader
import com.saadm.zenith.ui.components.ActionSheet
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.Lu
import com.saadm.zenith.ui.components.PrimaryButton
import com.saadm.zenith.ui.components.SegmentedControl
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.ZenithTextButton
import com.saadm.zenith.ui.components.ZenithTextField
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** The screens under Settings. */
enum class SettingsPage(val title: String) {
    Appearance("Appearance"),
    Animation("Screen animation"),
    MonthStart("Month starts on"),
    WeekStarts("Week starts on"),
    Split("Default split"),
    Accounts("Accounts"),
    Licences("Licences")
}

/** A library or font the app ships, and the file in assets/licenses holding its licence. */
data class Notice(val name: String, val licence: String, val file: String)

private const val APACHE = "Apache 2.0"

val notices = listOf(
    Notice("AndroidX and Jetpack Compose", APACHE, "Apache-2.0.txt"),
    Notice("Room, DataStore and Navigation", APACHE, "Apache-2.0.txt"),
    Notice("Kotlin and kotlinx.coroutines", APACHE, "Apache-2.0.txt"),
    Notice("JetBrains annotations", APACHE, "Apache-2.0.txt"),
    Notice("Dagger and Hilt", APACHE, "Apache-2.0.txt"),
    Notice("Guava", APACHE, "Apache-2.0.txt"),
    Notice("Okio", APACHE, "Apache-2.0.txt"),
    Notice("JSR 305, javax.inject, jakarta.inject", APACHE, "Apache-2.0.txt"),
    Notice("JSpecify", APACHE, "Apache-2.0.txt"),
    Notice("Lucide icons", "ISC and MIT", "ISC-Lucide.txt"),
    Notice("Compose Icons", "MIT", "MIT-ComposeIcons.txt"),
    Notice("Work Sans font", "SIL OFL 1.1", "OFL-WorkSans.txt")
)

// Licence files are wrapped at 70-odd columns; joined up, they wrap to the screen instead.
fun reflowLicence(text: String): String = text
    .split(Regex("\n[ \t]*\n"))
    .map { paragraph -> paragraph.lines().joinToString(" ") { it.trim() }.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("\n\n")

@Composable
fun SettingsPageScreen(page: SettingsPage, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(page.title, onBack)
        val loaded = state ?: return@Column
        val preferences = loaded.preferences
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(ZenithSpacing.space4)
                .padding(bottom = LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
        ) {
            when (page) {
                SettingsPage.Appearance -> {
                    Choices("Theme", ThemeMode.entries, preferences.themeMode, { it.name }, viewModel::setThemeMode)
                    Choices("Colour", ZenithPalette.entries, preferences.palette, { it.name }, viewModel::setPalette)
                }
                SettingsPage.Animation -> {
                    Choices("Style", TransitionStyle.entries, preferences.transitionStyle, { it.label }, viewModel::setTransitionStyle)
                    Choices(
                        "Speed",
                        TransitionDurationOptions,
                        normalizeTransitionDurationMillis(preferences.transitionDurationMillis),
                        { "$it ms" },
                        viewModel::setTransitionDuration
                    )
                }
                SettingsPage.MonthStart -> {
                    DayGrid(preferences.monthStartDay, viewModel::setMonthStartDay)
                }
                SettingsPage.WeekStarts ->
                    Choices(null, WeekStart.entries, preferences.weekStart, { weekStartLabels.getValue(it) }, viewModel::setWeekStart)
                SettingsPage.Split -> {
                    Choices(null, SplitMode.entries, preferences.defaultSplitMode, { splitModeLabels.getValue(it) }, viewModel::setSplitMode)
                }
                SettingsPage.Accounts -> Accounts(loaded, viewModel)
                SettingsPage.Licences -> Licences()
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = ZenithType.caption, color = zenithColors().inkMuted)
}

/** One tile of mutually exclusive rows, the chosen one ticked. */
@Composable
private fun <T> Choices(title: String?, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val rows: @Composable () -> Unit = {
        options.forEach { option -> ChoiceRow(label(option), option == selected) { onSelect(option) } }
    }
    if (title != null) SettingsGroup(title) { rows() } else ZenithCard { rows() }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = zenithColors()
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton) {
                if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .defaultMinSize(minHeight = 52.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ZenithType.body, color = colors.ink, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(painterResource(Lu.lucide_ic_check), contentDescription = null, tint = colors.link, modifier = Modifier.size(20.dp))
        }
    }
}

/** Days 1 to 28, seven to a row like a calendar. */
@Composable
private fun DayGrid(selected: Int, onSelect: (Int) -> Unit) {
    val colors = zenithColors()
    val view = LocalView.current
    ZenithCard {
        (MonthStartDay.MIN..MonthStartDay.MAX).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val chosen = day == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (chosen) colors.primaryStrong else colors.surfaceRaised)
                            .selectable(selected = chosen, role = Role.RadioButton) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSelect(day)
                            }
                            .semantics { contentDescription = "Day $day" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.toString(),
                            style = ZenithType.numeric,
                            color = if (chosen) colors.onStrong else colors.ink,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

private val accountTypeLabels = mapOf(
    AccountType.CASH to "Cash",
    AccountType.UPI to "UPI",
    AccountType.DEBIT_CARD to "Debit card",
    AccountType.CREDIT_CARD to "Credit card",
    AccountType.BANK_TRANSFER to "Bank"
)

@Composable
private fun Accounts(state: SettingsState, viewModel: SettingsViewModel) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var open by remember { mutableStateOf<AccountBalance?>(null) }
    if (state.accounts.isNotEmpty()) {
        ZenithCard {
            state.accounts.forEach { account ->
                ListRow(
                    title = account.name,
                    subtitle = accountTypeLabels[account.type],
                    trailing = { AmountText(account.balance) },
                    onClick = { open = account }
                )
            }
        }
    } else {
        Hint("No accounts yet.")
    }
    PrimaryButton("Add account", { adding = true }, Modifier.fillMaxWidth())
    if (state.accounts.isNotEmpty()) {
        val chosen = state.preferences.defaultAccountId
        Choices(
            "Default for new entries",
            listOf<Long?>(null) + state.accounts.map { it.id },
            chosen,
            { id -> state.accounts.firstOrNull { it.id == id }?.name ?: "None" },
            viewModel::setDefaultAccount
        )
    }

    if (adding) {
        AccountSheet(null, { adding = false }, { name, type, balance ->
            viewModel.saveAccount(null, name, type, balance)
            adding = false
        })
    }
    open?.let { existing ->
        AccountSheet(
            existing,
            { open = null },
            { name, type, balance ->
                viewModel.saveAccount(existing, name, type, balance)
                open = null
            },
            onArchive = {
                viewModel.archive(existing.id)
                open = null
            }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AccountSheet(
    existing: AccountBalance?,
    onDismiss: () -> Unit,
    onSave: (String, AccountType, Long) -> Unit,
    onArchive: (() -> Unit)? = null
) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: AccountType.BANK_TRANSFER) }
    var opening by rememberSaveable {
        mutableStateOf(existing?.openingBalance?.takeIf { it != 0L }?.let(MoneyFormatter::plain).orEmpty())
    }
    val balance = parseBalance(opening)
    ActionSheet(
        title = if (existing == null) "Add account" else "Edit account",
        confirmLabel = "Save",
        enabled = name.isNotBlank() && balance != null,
        onConfirm = { balance?.let { onSave(name, type, it) } },
        onDismiss = onDismiss
    ) {
        ZenithTextField(name, { name = it }, "Name")
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
        ) {
            accountTypeLabels.forEach { (value, label) -> FilterChip(label, type == value, { type = value }) }
        }
        ZenithTextField(
            opening,
            { opening = it },
            "Opening balance in ₹",
            error = if (balance == null) "Enter a number, with up to two decimals" else null
        )
        if (onArchive != null) {
            ZenithTextButton("Archive account", onArchive)
        }
    }
}

@Composable
private fun Licences() {
    val context = LocalContext.current
    var open by remember { mutableStateOf<Notice?>(null) }
    ZenithCard {
        notices.forEach { notice ->
            ListRow(
                title = notice.name,
                subtitle = notice.licence,
                trailing = {
                    Icon(
                        painterResource(Lu.lucide_ic_chevron_right),
                        contentDescription = null,
                        tint = zenithColors().inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = { open = notice }
            )
        }
    }
    open?.let { notice ->
        val text = remember(notice.file) {
            runCatching {
                context.assets.open("licenses/${notice.file}").bufferedReader().use { reflowLicence(it.readText()) }
            }.getOrDefault("The licence file could not be read.")
        }
        ActionSheet(title = notice.name, message = notice.licence, dismissLabel = "Close", onDismiss = { open = null }) {
            Text(text, style = ZenithType.caption, color = zenithColors().ink)
        }
    }
}
