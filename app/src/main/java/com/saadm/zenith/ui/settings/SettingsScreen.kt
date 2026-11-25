package com.saadm.zenith.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.preferences.AppPreferences
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.model.WeekStart
import com.saadm.zenith.ui.common.LocalBottomBarInset
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.components.ActionSheet
import com.saadm.zenith.ui.components.Avatar
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.Lu
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.ZenithTextField
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

private enum class SettingsDialog { Profile, BackupPassphrase, RestorePassphrase, DeleteAll, Help, Privacy }

private const val MIN_PASSPHRASE = 8

val splitModeLabels = mapOf(SplitMode.EQUAL to "Equally", SplitMode.BY_ITEMS to "By items", SplitMode.CUSTOM to "Custom")
val weekStartLabels = mapOf(WeekStart.MONDAY to "Monday", WeekStart.SUNDAY to "Sunday")

private class SettingsActions(
    val open: (SettingsDialog) -> Unit,
    val page: (SettingsPage) -> Unit,
    val exportCsv: () -> Unit,
    val exportJson: () -> Unit,
    val restore: () -> Unit,
    val clearCache: () -> Unit,
    val categories: () -> Unit,
    val payees: () -> Unit,
    val budgets: () -> Unit,
    val fillCategory: (Boolean) -> Unit
)

@Composable
fun SettingsScreen(
    onOpenPage: (SettingsPage) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenPayees: () -> Unit,
    onOpenBudgets: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
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
        page = onOpenPage,
        exportCsv = { csvPicker.launch("zenith-transactions.csv") },
        exportJson = { jsonPicker.launch("zenith-export.json") },
        restore = { restorePicker.launch(arrayOf("*/*")) },
        clearCache = viewModel::clearCache,
        categories = onOpenCategories,
        payees = onOpenPayees,
        budgets = onOpenBudgets,
        fillCategory = viewModel::setFillCategory
    )
    SettingsContent(
        state = loaded,
        actions = actions,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ZenithSpacing.space4)
            .padding(bottom = LocalBottomBarInset.current)
    )

    val close = { dialog = null }
    when (dialog) {
        SettingsDialog.Profile -> ProfileSheet(loaded.preferences, close) { name, upi ->
            viewModel.setProfile(name, upi)
            close()
        }
        SettingsDialog.BackupPassphrase -> PassphraseSheet(
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
        SettingsDialog.RestorePassphrase -> PassphraseSheet(
            title = "Replace everything?",
            body = "All entries, people, accounts, categories and budgets on this device are replaced by " +
                "the backup. Back up first if you want to keep them.",
            confirm = "Replace everything",
            minLength = 1,
            destructive = true,
            onDismiss = close
        ) { typed ->
            restoreFile?.let { viewModel.restore(it, typed) }
            close()
        }
        SettingsDialog.DeleteAll -> DeleteAllSheet(close) {
            viewModel.deleteAll(it)
            close()
        }
        SettingsDialog.Help -> ActionSheet(
            title = "Help",
            message = "Tap + to add an expense, income or money between friends. Insights shows the month, " +
                "budgets and shared balances. Everything stays on this device, so back up from Settings " +
                "before changing phones.",
            dismissLabel = "Close",
            onDismiss = close
        )
        SettingsDialog.Privacy -> ActionSheet(
            title = "Privacy",
            message = "Zenith has no account, no sign-in, no bank link and no internet permission. Your data is " +
                "stored on this device. It leaves when you export, back up or share it yourself. If Android's " +
                "backup is turned on for this phone, Android may also include it in your Google account backup " +
                "or in a transfer to a new phone.",
            dismissLabel = "Close",
            onDismiss = close
        )
        null -> Unit
    }

    message?.let { ActionSheet(title = "Zenith", message = it, dismissLabel = "Close", onDismiss = viewModel::dismissMessage) }
}

@Composable
private fun SettingsContent(state: SettingsState, actions: SettingsActions, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val preferences = state.preferences
    val context = LocalContext.current
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

        SettingsGroup("General") {
            SettingsRow(
                Lu.lucide_ic_palette, Tile.Indigo, "Appearance",
                value = preferences.themeMode.name,
                onClick = { actions.page(SettingsPage.Appearance) }
            )
            SettingsRow(
                Lu.lucide_ic_sparkles, Tile.Plum, "Screen animation",
                value = "${normalizeTransitionDurationMillis(preferences.transitionDurationMillis)} ms",
                onClick = { actions.page(SettingsPage.Animation) }
            )
            SettingsRow(
                Lu.lucide_ic_calendar_days, Tile.Orange, "Month starts on",
                value = "Day ${preferences.monthStartDay}",
                onClick = { actions.page(SettingsPage.MonthStart) }
            )
            SettingsRow(
                Lu.lucide_ic_calendar_range, Tile.Blue, "Week starts on",
                value = weekStartLabels[preferences.weekStart],
                onClick = { actions.page(SettingsPage.WeekStarts) }
            )
            SettingsRow(
                Lu.lucide_ic_split, Tile.Teal, "Default split",
                value = splitModeLabels[preferences.defaultSplitMode],
                onClick = { actions.page(SettingsPage.Split) }
            )
            SettingsSwitchRow(
                Lu.lucide_ic_wand_sparkles, Tile.Gold, "Suggest category",
                checked = preferences.fillCategoryFromPayee,
                onCheckedChange = actions.fillCategory
            )
        }

        SettingsGroup("Money") {
            SettingsRow(
                Lu.lucide_ic_wallet, Tile.Green, "Accounts",
                value = state.accounts.size.toString(),
                onClick = { actions.page(SettingsPage.Accounts) }
            )
            SettingsRow(Lu.lucide_ic_tag, Tile.Indigo, "Categories", onClick = actions.categories)
            SettingsRow(Lu.lucide_ic_target, Tile.Orange, "Budgets", onClick = actions.budgets)
            SettingsRow(Lu.lucide_ic_users, Tile.Blue, "People", onClick = actions.payees)
        }

        SettingsGroup("Data") {
            SettingsRow(Lu.lucide_ic_shield, Tile.Green, "Privacy", onClick = { actions.open(SettingsDialog.Privacy) })
            SettingsRow(Lu.lucide_ic_file_text, Tile.Teal, "Export transactions", value = "CSV", onClick = actions.exportCsv)
            SettingsRow(Lu.lucide_ic_download, Tile.Plum, "Export everything", value = "JSON", onClick = actions.exportJson)
            SettingsRow(Lu.lucide_ic_upload, Tile.Blue, "Back up to a file", onClick = { actions.open(SettingsDialog.BackupPassphrase) })
            SettingsRow(Lu.lucide_ic_archive_restore, Tile.Gold, "Restore from a backup", onClick = actions.restore)
            SettingsRow(Lu.lucide_ic_brush, Tile.Indigo, "Clear cache", onClick = actions.clearCache)
            SettingsRow(Lu.lucide_ic_trash_2, Tile.Red, "Delete all data", onClick = { actions.open(SettingsDialog.DeleteAll) })
        }

        SettingsGroup("About") {
            val version = remember {
                runCatching {
                    val info = context.packageManager.getPackageInfo(context.packageName, 0)
                    "${info.versionName} (build ${info.longVersionCode})"
                }.getOrNull().orEmpty()
            }
            SettingsRow(Lu.lucide_ic_circle_question_mark, Tile.Blue, "Help", onClick = { actions.open(SettingsDialog.Help) })
            SettingsRow(Lu.lucide_ic_scale, Tile.Teal, "Open-source licences", onClick = { actions.page(SettingsPage.Licences) })
            SettingsRow(Lu.lucide_ic_info, Tile.Indigo, "Version", value = version)
        }
        DebugTools()
        Text(
            "Created with love by Saad",
            style = ZenithType.caption,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = ZenithSpacing.space4)
        )
    }
}

@Composable
private fun ProfileSheet(preferences: AppPreferences, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(preferences.profileName) }
    var upi by rememberSaveable { mutableStateOf(preferences.upiId) }
    ActionSheet(title = "Profile", confirmLabel = "Save", onConfirm = { onSave(name, upi) }, onDismiss = onDismiss) {
        ZenithTextField(name, { name = it }, "Name")
        ZenithTextField(upi, { upi = it }, "UPI ID (optional)")
    }
}

@Composable
private fun PassphraseSheet(
    title: String,
    body: String,
    confirm: String,
    minLength: Int,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    ActionSheet(
        title = title,
        message = body,
        confirmLabel = confirm,
        enabled = passphrase.length >= minLength,
        destructive = destructive,
        onConfirm = { onConfirm(passphrase) },
        onDismiss = onDismiss
    ) {
        ZenithTextField(
            passphrase,
            { passphrase = it },
            "Passphrase",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation()
        )
        if (minLength > 1) {
            Text("At least $minLength characters", style = ZenithType.caption, color = zenithColors().inkMuted)
        }
    }
}

@Composable
private fun DeleteAllSheet(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var typed by remember { mutableStateOf("") }
    ActionSheet(
        title = "Delete all data?",
        message = "Every entry, person, account and budget is removed, and settings reset. This cannot be undone. " +
            "Back up first if you might want any of it.",
        confirmLabel = "Delete everything",
        enabled = typed == "DELETE",
        destructive = true,
        onConfirm = { onConfirm(typed) },
        onDismiss = onDismiss
    ) {
        ZenithTextField(typed, { typed = it }, "Type DELETE")
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    PalettePreview { SettingsContent(SettingsState(AppPreferences(), emptyList()), actions) }
}
