package com.saadm.zenith.ui.settings

import com.saadm.zenith.domain.repository.CategoryRepo
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.entity.CategoryEntity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.preferences.AppPreferences
import com.saadm.zenith.data.preferences.AppPreferencesStore
import com.saadm.zenith.domain.backup.LocalBackup
import com.saadm.zenith.domain.export.DataTransfer
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.model.WeekStart
import com.saadm.zenith.domain.repository.AccountRepo
import com.saadm.zenith.domain.usecase.ArchiveAccount
import com.saadm.zenith.domain.usecase.SaveAccount
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

data class SettingsState(
    val preferences: AppPreferences,
    val accounts: List<AccountBalance>,
    val categories: List<CategoryEntity> = emptyList()
)

/** A typed opening balance in paise. Blank is zero and a leading minus is money owed. Null is not a number. */
fun parseBalance(input: String): Long? {
    val text = input.trim()
    val negative = text.startsWith("-") || text.startsWith("−")
    val digits = if (negative) text.drop(1).trim() else text
    if (digits.isEmpty()) return if (negative) null else 0L
    if (digits.all { it == '0' || it == '.' || it == ',' } && digits.count { it == '.' } <= 1) return 0L
    val paise = MoneyFormatter.parse(digits) ?: return null
    return if (negative) -paise else paise
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: AppPreferencesStore,
    accounts: AccountRepo,
    categories: CategoryRepo,
    private val saveAccount: SaveAccount,
    private val archiveAccount: ArchiveAccount,
    private val transfer: DataTransfer,
    private val backup: LocalBackup,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val state: StateFlow<SettingsState?> = combine(
        store.preferencesFlow,
        accounts.observeBalances(),
        categories.observeAll()
    ) { p, a, c ->
        SettingsState(p, a, c.filter { !it.isDeleted })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)

    /** The outcome of the last export, backup, restore or delete. */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun dismissMessage() {
        _message.value = null
    }

    private fun update(block: suspend AppPreferencesStore.() -> Unit) {
        viewModelScope.launch { store.block() }
    }

    fun setProfile(name: String, upiId: String) = update { updateProfile(name, upiId) }
    fun setThemeMode(mode: ThemeMode) = update { updateThemeMode(mode) }
    fun setPalette(palette: ZenithPalette) = update { updatePalette(palette) }
    fun setTransitionDuration(millis: Int) = update { updateTransitionDurationMillis(millis) }
    fun setTransitionStyle(style: TransitionStyle) = update { updateTransitionStyle(style) }
    fun setWeekStart(start: WeekStart) = update { updateWeekStart(start) }
    fun setMonthStartDay(day: Int) = update { updateMonthStartDay(day) }
    fun setFillCategory(enabled: Boolean) = update { updateFillCategoryFromPayee(enabled) }
    fun setSplitMode(mode: SplitMode) = update { updateDefaultSplitMode(mode) }
    fun setDefaultCategory(type: TxnType, id: Long) = update { updateDefaultCategory(type, id) }
    fun setDefaultAccount(id: Long?) = update { updateDefaultAccountId(id) }

    /** [existing] is null for a new account. */
    fun saveAccount(existing: AccountBalance?, name: String, type: AccountType, openingBalance: Long) {
        if (name.isBlank()) return
        val order = existing?.sortOrder ?: ((state.value?.accounts?.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        viewModelScope.launch {
            saveAccount(
                AccountEntity(
                    id = existing?.id ?: 0,
                    name = name,
                    type = type,
                    colorHex = existing?.colorHex ?: "#777FCF",
                    emoji = existing?.emoji ?: "",
                    sortOrder = order,
                    openingBalance = openingBalance
                )
            )
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch {
            archiveAccount(id)
            if (state.value?.preferences?.defaultAccountId == id) store.updateDefaultAccountId(null)
        }
    }

    // File work runs off the main thread and reports what happened; a failure never crashes the screen.
    private fun run(failure: String, block: suspend () -> String) {
        viewModelScope.launch {
            _message.value = try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is IllegalArgumentException) e.message ?: failure else failure
            }
        }
    }

    private fun write(uri: Uri): OutputStream =
        context.contentResolver.openOutputStream(uri, "wt") ?: error("The file could not be opened")

    private fun read(uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri) ?: error("The file could not be opened")

    fun exportCsv(uri: Uri) = run("Export failed") {
        write(uri).use { transfer.writeCsv(it) }
        "Transactions exported"
    }

    fun exportJson(uri: Uri) = run("Export failed") {
        write(uri).use { transfer.writeJson(it) }
        "Everything exported"
    }

    fun backUp(uri: Uri, passphrase: String) = run("Backup failed") {
        write(uri).use { backup.writeBackup(it, passphrase.toCharArray()) }
        "Backup saved"
    }

    fun restore(uri: Uri, passphrase: String) = run("Restore failed. Nothing was changed.") {
        read(uri).use { backup.restoreBackup(it, passphrase.toCharArray(), replaceEverything = true) }
        "Backup restored"
    }

    fun deleteAll(confirmation: String) = run("Delete failed") {
        backup.deleteAllData(confirmation)
        "All data deleted"
    }

    fun clearCache() = run("Could not clear the cache") {
        "Cache cleared · ${backup.clearCache() / 1024} KB freed"
    }
}
