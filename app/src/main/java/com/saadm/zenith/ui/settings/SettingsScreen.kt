package com.saadm.zenith.ui.settings

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.preferences.AppPreferences
import com.saadm.zenith.domain.model.MonthStartDay
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.model.WeekStart
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.Avatar
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.SegmentedControl
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.ZenithSwitch
import com.saadm.zenith.ui.components.ZenithTextButton
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

private enum class SettingsDialog {
    Profile, NewAccount, EditAccount, BackupPassphrase, RestorePassphrase, DeleteAll, Help, Privacy, Licences
}

private const val MIN_PASSPHRASE = 8

private val accountTypeLabels = mapOf(
    AccountType.CASH to "Cash",
    AccountType.UPI to "UPI",
    AccountType.DEBIT_CARD to "Debit card",
    AccountType.CREDIT_CARD to "Credit card",
    AccountType.BANK_TRANSFER to "Bank"
)

private class SettingsActions(
    val open: (SettingsDialog) -> Unit,
    val editAccount: (AccountBalance) -> Unit,
    val exportCsv: () -> Unit,
    val exportJson: () -> Unit,
    val restore: () -> Unit,
    val clearCache: () -> Unit,
    val categories: () -> Unit,
    val payees: () -> Unit,
    val budgets: () -> Unit
)

@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenPayees: () -> Unit,
    onOpenBudgets: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    var account by remember { mutableStateOf<AccountBalance?>(null) }
    // Held in memory only, so a passphrase is never written into saved state.
    var passphrase by remember { mutableStateOf("") }
    var restoreFile by remember { mutableStateOf<android.net.Uri?>(null) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportCsv)
    }
    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportJson)
    }
    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && passphrase.length >= MIN_PASSPHRASE) viewModel.backUp(uri, passphrase)
        passphrase = ""
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            restoreFile = uri
            dialog = SettingsDialog.RestorePassphrase
        }
    }

    val loaded = state ?: return
    val actions = SettingsActions(
        open = { dialog = it },
        editAccount = {
            account = it
            dialog = SettingsDialog.EditAccount
        },
        exportCsv = { csvPicker.launch("zenith-transactions.csv") },
        exportJson = { jsonPicker.launch("zenith-export.json") },
        restore = { restorePicker.launch(arrayOf("*/*")) },
        clearCache = viewModel::clearCache,
        categories = onOpenCategories,
        payees = onOpenPayees,
        budgets = onOpenBudgets
    )
    SettingsContent(
        state = loaded,
        actions = actions,
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ZenithSpacing.space4)
    )

    val close = { dialog = null }
    when (dialog) {
        SettingsDialog.Profile -> ProfileDialog(loaded.preferences, close) { name, upi ->
            viewModel.setProfile(name, upi)
            close()
        }
        SettingsDialog.NewAccount -> AccountDialog(null, close, { name, type, balance ->
            viewModel.saveAccount(null, name, type, balance)
            close()
        })
        SettingsDialog.EditAccount -> account?.let { existing ->
            AccountDialog(
                existing,
                close,
                { name, type, balance ->
                    viewModel.saveAccount(existing, name, type, balance)
                    close()
                },
                onArchive = {
                    viewModel.archive(existing.id)
                    close()
                }
            )
        }
        SettingsDialog.BackupPassphrase -> PassphraseDialog(
            title = "Back up",
            body = "The backup is encrypted. Keep the passphrase: it cannot be recovered.",
            confirm = "Choose file",
            minLength = MIN_PASSPHRASE,
            onDismiss = close
        ) {
            passphrase = it
            close()
            backupPicker.launch("zenith.zbackup")
        }
        SettingsDialog.RestorePassphrase -> PassphraseDialog(
            title = "Restore and replace everything?",
            body = "All entries, people, accounts, categories and budgets on this device are replaced by " +
                "the backup. Back up first if you want to keep them.",
            confirm = "Replace everything",
            minLength = 1,
            onDismiss = close
        ) { typed ->
            restoreFile?.let { viewModel.restore(it, typed) }
            close()
        }
        SettingsDialog.DeleteAll -> DeleteAllDialog(close) {
            viewModel.deleteAll(it)
            close()
        }
        SettingsDialog.Help -> TextDialog(
            "Help",
            "Tap + to add an expense, income or money between friends. Insights shows the month, " +
                "budgets and shared balances. Everything stays on this device, so back up from Settings " +
                "before changing phones.",
            close
        )
        SettingsDialog.Privacy -> TextDialog(
            "Privacy",
            "Zenith has no account, no sign-in and no bank link. Your data is stored on this device. " +
                "It leaves only when you export, back up or share it yourself.",
            close
        )
        SettingsDialog.Licences -> LicencesDialog(close)
        null -> Unit
    }

    message?.let { TextDialog("Zenith", it, viewModel::dismissMessage) }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space1)) {
        Text(
            title,
            style = ZenithType.subheading,
            color = zenithColors().ink,
            modifier = Modifier.padding(top = ZenithSpacing.space3).semantics { heading() }
        )
        ZenithCard(content = content)
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = ZenithType.bodyStrong,
        color = zenithColors().ink,
        modifier = Modifier.padding(top = ZenithSpacing.space2)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) { content() }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    actions: SettingsActions,
    viewModel: SettingsViewModel?,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val preferences = state.preferences
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        Text(
            "Settings",
            style = ZenithType.heading,
            color = colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        ZenithCard {
            val name = preferences.profileName.ifBlank { "Your name" }
            ListRow(
                title = name,
                subtitle = preferences.upiId.ifBlank { "Add a UPI ID for payment requests" },
                leading = { Avatar(name, Modifier.clearAndSetSemantics {}) },
                onClick = { actions.open(SettingsDialog.Profile) }
            )
        }
        Text("Stored on this device · works offline", style = ZenithType.caption, color = colors.inkMuted)

        Section("App preferences") {
            Label("Theme")
            SegmentedControl(
                ThemeMode.entries.map { it.name },
                preferences.themeMode.ordinal,
                { viewModel?.setThemeMode(ThemeMode.entries[it]) }
            )
            Label("Colour")
            SegmentedControl(
                listOf("Colour", "Monochrome"),
                preferences.palette.ordinal,
                { viewModel?.setPalette(ZenithPalette.entries[it]) }
            )
            Label("Screen animation")
            Chips {
                TransitionStyle.entries.forEach { style ->
                    FilterChip(style.label, preferences.transitionStyle == style, { viewModel?.setTransitionStyle(style) })
                }
            }
            Chips {
                val current = normalizeTransitionDurationMillis(preferences.transitionDurationMillis)
                TransitionDurationOptions.forEach { millis ->
                    FilterChip("$millis ms", current == millis, { viewModel?.setTransitionDuration(millis) })
                }
            }
            Label("Month starts on day ${preferences.monthStartDay}")
            Chips {
                SecondaryButton(
                    "Earlier",
                    { viewModel?.setMonthStartDay(preferences.monthStartDay - 1) },
                    enabled = preferences.monthStartDay > MonthStartDay.MIN
                )
                SecondaryButton(
                    "Later",
                    { viewModel?.setMonthStartDay(preferences.monthStartDay + 1) },
                    enabled = preferences.monthStartDay < MonthStartDay.MAX
                )
            }
            Label("Week starts on")
            SegmentedControl(
                listOf("Monday", "Sunday"),
                preferences.weekStart.ordinal,
                { viewModel?.setWeekStart(WeekStart.entries[it]) }
            )
            Label("Default split")
            SegmentedControl(
                listOf("Equally", "By items", "Custom"),
                preferences.defaultSplitMode.ordinal,
                { viewModel?.setSplitMode(SplitMode.entries[it]) }
            )
            ZenithSwitch(
                "Suggest a category from the merchant",
                preferences.fillCategoryFromPayee,
                { viewModel?.setFillCategory(it) }
            )
        }

        Section("Financial setup") {
            state.accounts.forEach { account ->
                ListRow(
                    title = account.name,
                    subtitle = accountTypeLabels[account.type],
                    trailing = { AmountText(account.balance) },
                    onClick = { actions.editAccount(account) }
                )
            }
            SecondaryButton("Add account", { actions.open(SettingsDialog.NewAccount) }, Modifier.fillMaxWidth())
            if (state.accounts.isNotEmpty()) {
                Label("Default account for new entries")
                Chips {
                    FilterChip("None", preferences.defaultAccountId == null, { viewModel?.setDefaultAccount(null) })
                    state.accounts.forEach {
                        FilterChip(it.name, preferences.defaultAccountId == it.id, { viewModel?.setDefaultAccount(it.id) })
                    }
                }
            }
            ListRow(title = "Categories", onClick = actions.categories)
            ListRow(title = "Budgets", onClick = actions.budgets)
            ListRow(title = "People", onClick = actions.payees)
        }

        Section("Data and privacy") {
            ListRow(title = "Privacy", subtitle = "What stays on this device", onClick = { actions.open(SettingsDialog.Privacy) })
            ListRow(title = "Export transactions", subtitle = "CSV spreadsheet", onClick = actions.exportCsv)
            ListRow(title = "Export everything", subtitle = "JSON, not encrypted", onClick = actions.exportJson)
        }

        Section("Back up") {
            ListRow(
                title = "Back up to a file",
                subtitle = "Encrypted with your passphrase",
                onClick = { actions.open(SettingsDialog.BackupPassphrase) }
            )
        }

        Section("Restore") {
            ListRow(title = "Restore from a backup", subtitle = "Replaces everything", onClick = actions.restore)
        }

        Section("Local data") {
            ListRow(title = "Clear cache", subtitle = "Temporary files only", onClick = actions.clearCache)
            ListRow(
                title = "Delete all data",
                subtitle = "Permanent",
                onClick = { actions.open(SettingsDialog.DeleteAll) }
            )
        }

        Section("About") {
            val context = LocalContext.current
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                    .getOrNull().orEmpty()
            }
            ListRow(title = "Version", subtitle = version)
            ListRow(title = "Help", onClick = { actions.open(SettingsDialog.Help) })
            ListRow(title = "Open-source licences", onClick = { actions.open(SettingsDialog.Licences) })
        }
    }
}

@Composable
private fun FormDialog(
    title: String,
    confirm: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = zenithColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceRaised,
        title = { Text(title, style = ZenithType.subheading, color = colors.ink) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
                content = content
            )
        },
        confirmButton = { TextButton(enabled = enabled, onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TextDialog(title: String, body: String, onDismiss: () -> Unit) {
    val colors = zenithColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceRaised,
        title = { Text(title, style = ZenithType.subheading, color = colors.ink) },
        text = { Text(body, style = ZenithType.body, color = colors.ink, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, secret: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        }
    )
}

@Composable
private fun ProfileDialog(preferences: AppPreferences, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(preferences.profileName) }
    var upi by rememberSaveable { mutableStateOf(preferences.upiId) }
    FormDialog("Profile", "Save", true, { onSave(name, upi) }, onDismiss) {
        Field(name, { name = it }, "Name")
        Field(upi, { upi = it }, "UPI ID (optional)")
    }
}

@Composable
private fun AccountDialog(
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
    FormDialog(
        title = if (existing == null) "Add account" else "Edit account",
        confirm = "Save",
        enabled = name.isNotBlank() && balance != null,
        onConfirm = { balance?.let { onSave(name, type, it) } },
        onDismiss = onDismiss
    ) {
        Field(name, { name = it }, "Name")
        Chips { accountTypeLabels.forEach { (value, label) -> FilterChip(label, type == value, { type = value }) } }
        Field(opening, { opening = it }, "Opening balance in ₹")
        if (balance == null) Text("Enter a number, with up to two decimals", style = ZenithType.caption)
        if (onArchive != null) {
            Text("Archiving hides the account. Its entries stay.", style = ZenithType.caption)
            ZenithTextButton("Archive account", onArchive)
        }
    }
}

@Composable
private fun PassphraseDialog(
    title: String,
    body: String,
    confirm: String,
    minLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    FormDialog(title, confirm, passphrase.length >= minLength, { onConfirm(passphrase) }, onDismiss) {
        Text(body, style = ZenithType.body)
        Field(passphrase, { passphrase = it }, "Passphrase", secret = true)
        if (minLength > 1) Text("At least $minLength characters", style = ZenithType.caption)
    }
}

@Composable
private fun DeleteAllDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var typed by remember { mutableStateOf("") }
    FormDialog("Delete all data?", "Delete everything", typed == "DELETE", { onConfirm(typed) }, onDismiss) {
        Text(
            "Every entry, person, account and budget is removed, and settings reset. This cannot be undone. " +
                "Back up first if you might want any of it.",
            style = ZenithType.body
        )
        Field(typed, { typed = it }, "Type DELETE")
    }
}

@Composable
private fun LicencesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember {
        val libraries = "AndroidX, Jetpack Compose, Room, DataStore, Hilt, Kotlin and Compose Unstyled are used " +
            "under the Apache License 2.0 (apache.org/licenses/LICENSE-2.0). Compose Unstyled is MIT licensed."
        val fonts = runCatching {
            context.assets.list("licenses").orEmpty().joinToString("\n\n") { name ->
                context.assets.open("licenses/$name").bufferedReader().use { it.readText() }
            }
        }.getOrDefault("")
        "$libraries\n\n$fonts"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = zenithColors().surfaceRaised,
        title = { Text("Open-source licences", style = ZenithType.subheading) },
        text = { Text(text, style = ZenithType.caption, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Preview
@Composable
private fun SettingsPreview() {
    val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {})
    PalettePreview { SettingsContent(SettingsState(AppPreferences(), emptyList()), actions, null) }
}
