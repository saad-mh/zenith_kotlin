package com.saadm.zenith.ui.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.preferences.SettingsRepository
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.repository.AccountRepo
import com.saadm.zenith.domain.repository.CategoryRepo
import com.saadm.zenith.domain.repository.PayeeRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import com.saadm.zenith.domain.split.Owed
import com.saadm.zenith.domain.split.paymentRequest
import com.saadm.zenith.ui.common.toLocalDate
import com.saadm.zenith.ui.navigation.TXN_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AddOptions(
    val categories: List<CategoryEntity> = emptyList(),
    val payees: List<Payee> = emptyList(),
    val accounts: List<AccountBalance> = emptyList(),
    val history: List<TransactionEntity> = emptyList()
) {
    /** Categories of the form's kind, the most recently used first. */
    fun categoriesFor(form: AddForm): List<CategoryEntity> {
        val type = form.categoryType ?: return emptyList()
        val lastUsed = history.groupBy { it.categoryId }.mapValues { (_, rows) -> rows.maxOf { it.transactedAt } }
        return categories
            .filter { it.txnType == type && !it.isDeleted }
            .sortedWith(compareByDescending<CategoryEntity> { lastUsed[it.id] ?: 0L }.thenBy { it.sortOrder })
    }
}

/** [request] is a message to share, when the user asked for one. */
data class Saved(val request: String? = null)

@HiltViewModel
class AddTxnViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val transactions: TransactionRepo,
    private val categories: CategoryRepo,
    private val payees: PayeeRepo,
    private val accounts: AccountRepo,
    private val settings: SettingsRepository,
    private val draft: AddDraft
) : ViewModel() {

    val today: LocalDate = LocalDate.now()

    private val editId: Long? = savedState.get<Long>(TXN_ID_ARG)?.takeIf { it > 0 }
    val isEdit: Boolean get() = editId != null

    /** The row being edited, once loaded. */
    private var original: TransactionEntity? = null

    private val _form = MutableStateFlow(if (editId == null) draft.form ?: AddForm(today) else AddForm(today))
    val form: StateFlow<AddForm> = _form.asStateFlow()

    private val _saved = MutableStateFlow<Saved?>(null)

    /** Set once the entry is written, so the sheet can close. */
    val saved: StateFlow<Saved?> = _saved.asStateFlow()

    private var saving = false

    val options: StateFlow<AddOptions> = combine(
        categories.observeAll(),
        payees.observeAllActive(),
        accounts.observeBalances(),
        transactions.observeAllActive()
    ) { c, p, a, t -> AddOptions(c, p, a, t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddOptions())

    init {
        viewModelScope.launch {
            if (editId != null) load(editId) else applyDefaultAccount()
        }
    }

    private suspend fun load(id: Long) {
        val row = transactions.observeAllActive().first().firstOrNull { it.id == id } ?: return
        original = row
        _form.value = AddForm(
            date = row.transactedAt.toLocalDate(),
            type = row.type,
            amount = MoneyFormatter.plain(row.amount),
            categoryId = row.categoryId,
            accountId = row.accountId,
            payeeId = row.payeeId,
            text = row.note.orEmpty(),
            categoryPicked = true
        )
    }

    private suspend fun applyDefaultAccount() {
        val preferences = settings.settings.first()
        val untouched = draft.form == null
        _form.update {
            it.copy(
                accountId = it.accountId ?: preferences.defaultAccountId,
                splitMode = if (untouched) preferences.defaultSplitMode else it.splitMode
            )
        }
    }

    fun change(transform: (AddForm) -> AddForm) {
        _form.update(transform)
        if (editId == null) draft.form = _form.value
    }

    fun setText(text: String) {
        change { it.copy(text = text) }
        viewModelScope.launch {
            if (!settings.settings.first().fillCategoryFromPayee) return@launch
            val now = _form.value
            if (now.categoryPicked) return@launch
            val allowed = options.value.categoriesFor(now).map { it.id }.toSet()
            suggestCategory(now.text, options.value.history, allowed)?.let { id -> change { it.copy(categoryId = id) } }
        }
    }

    /** Creates a person, then lets [onAdded] put them into the form. */
    fun addPayee(name: String, onAdded: (AddForm, Long) -> AddForm) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = payees.upsert(
                PayeeEntity(name = clean, avatarUri = null, phone = null, upiId = null, createdAt = System.currentTimeMillis())
            )
            change { onAdded(it, id) }
        }
    }

    /** The form as it can safely be written. An edited row keeps the ids it already had. */
    private suspend fun checked(form: AddForm): AddForm {
        val row = original
        return form.withKnownIds(
            accounts = accounts.observeBalances().first().map { it.id }.toSet() + listOfNotNull(row?.accountId),
            payees = payees.observeAllActive().first().map { it.id }.toSet() + listOfNotNull(row?.payeeId)
        )
    }

    /** Saves the expense with a due for each person's share, as one write. */
    fun saveSplit() {
        val typed = _form.value
        val total = typed.paise ?: return
        if (editId != null || saving || typed.type != TxnType.EXPENSE) return
        saving = true
        viewModelScope.launch {
            val form = checked(typed)
            val shares = form.shares()?.filterValues { it > 0 }.orEmpty()
            if (shares.isEmpty()) {
                _form.value = form
                saving = false
                return@launch
            }
            val now = System.currentTimeMillis()
            val at = timeOn(form.date, now)
            val category = categoryFor(form)
            val note = form.text.trim().ifEmpty { null }
            // The shared createdAt and transactedAt are what mark these rows as one split.
            fun row(amount: Long, type: TxnType, payeeId: Long?, accountId: Long?) = TransactionEntity(
                amount = amount, type = type, transactedAt = at, createdAt = now, updatedAt = now,
                categoryId = category, payeeId = payeeId, accountId = accountId, note = note, receiptUri = null
            )
            transactions.saveAll(
                listOf(row(total, TxnType.EXPENSE, null, form.accountId)) +
                    shares.map { (payeeId, amount) -> row(amount, TxnType.DUE_TO, payeeId, null) }
            )
            val request = if (form.sendRequest) {
                val preferences = settings.settings.first()
                val names = options.value.payees.associate { it.id to it.name }
                paymentRequest(
                    owed = shares.map { (id, amount) -> Owed(names[id] ?: "Someone", amount) },
                    about = note ?: "an expense of ${MoneyFormatter.format(total).text}",
                    upiId = preferences.upiId,
                    payeeName = preferences.profileName
                )
            } else null
            draft.form = null
            _saved.value = Saved(request)
        }
    }

    fun save() {
        val typed = _form.value
        val paise = typed.paise ?: return
        if (typed.problem != null || saving) return
        if (editId != null && original == null) return
        saving = true
        viewModelScope.launch {
            val form = checked(typed)
            if (form.problem != null) {
                // The chosen person is gone: show the form again so another can be picked.
                _form.value = form
                saving = false
                return@launch
            }
            val now = System.currentTimeMillis()
            val row = original
            transactions.save(
                TransactionEntity(
                    id = row?.id ?: 0,
                    amount = paise,
                    type = form.type,
                    transactedAt = timeOn(form.date, row?.transactedAt ?: now),
                    createdAt = row?.createdAt ?: now,
                    updatedAt = now,
                    categoryId = categoryFor(form),
                    payeeId = form.payeeId.takeIf { form.needsPayee },
                    accountId = form.accountId,
                    note = form.text.trim().ifEmpty { null },
                    receiptUri = row?.receiptUri,
                    currency = row?.currency ?: "INR"
                )
            )
            if (editId == null) draft.form = null
            _saved.value = Saved()
        }
    }

    /** The picked category if it still fits the type, else that type's default. */
    private suspend fun categoryFor(form: AddForm): Long {
        val type = form.categoryType
        if (type != null) {
            val all = categories.observeAll().first()
            all.firstOrNull { it.id == form.categoryId && it.txnType == type }?.let { return it.id }
        }
        val fallback = type ?: if (form.type == TxnType.SETTLEMENT_FROM) TxnType.INCOME else TxnType.EXPENSE
        return categories.defaultCategoryId(fallback) ?: error("No default category")
    }

    // The chosen date, at the time of day [keep] had.
    private fun timeOn(date: LocalDate, keep: Long): Long {
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(keep).atZone(zone).toLocalTime()
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}
