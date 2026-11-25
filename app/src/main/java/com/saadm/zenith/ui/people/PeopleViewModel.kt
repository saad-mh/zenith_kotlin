package com.saadm.zenith.ui.people

import kotlinx.coroutines.flow.map
import com.saadm.zenith.data.preferences.AppPreferencesStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.payee.BalanceDirection
import com.saadm.zenith.domain.payee.PayeeLedger
import com.saadm.zenith.domain.repository.AccountRepo
import com.saadm.zenith.domain.repository.PayeeRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import com.saadm.zenith.domain.usecase.RecordSettlement
import com.saadm.zenith.ui.navigation.PAYEE_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class PayeeRow(val payee: Payee, val ledger: PayeeLedger)

enum class PayeeFilter(val label: String, val direction: BalanceDirection?) {
    All("All", null),
    OwesYou("Owes you", BalanceDirection.OWES_YOU),
    YouOwe("You owe", BalanceDirection.YOU_OWE),
    Settled("Settled", BalanceDirection.SETTLED)
}

/** One titled group of the list. [subtotal] is the sum of its balances. */
data class PayeeSection(val title: String, val subtotal: Long, val rows: List<PayeeRow>)

/** The rows matching [filter] and [query], grouped by which way the money runs. */
fun payeeSections(rows: List<PayeeRow>, filter: PayeeFilter, query: String): List<PayeeSection> {
    val wanted = query.trim()
    val shown = rows.filter {
        (filter.direction == null || it.ledger.direction == filter.direction) &&
            it.payee.name.contains(wanted, ignoreCase = true)
    }
    return listOf(
        BalanceDirection.OWES_YOU to "Owes you",
        BalanceDirection.YOU_OWE to "You owe",
        BalanceDirection.SETTLED to "Settled"
    ).mapNotNull { (direction, title) ->
        val group = shown.filter { it.ledger.direction == direction }
        if (group.isEmpty()) null else PayeeSection(title, group.sumOf { it.ledger.net }, group)
    }
}

/** The balance once a settlement is saved. Positive means the person still owes the user. */
fun balanceAfter(net: Long, theyPaidYou: Boolean, amount: Long): Long = if (theyPaidYou) net - amount else net + amount

private fun Payee.toEntity(name: String, phone: String?, upiId: String?) =
    PayeeEntity(id = id, name = name, avatarUri = avatarUri, phone = phone, upiId = upiId, createdAt = createdAt)

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val payeeRepo: PayeeRepo,
    transactions: TransactionRepo
) : ViewModel() {

    /** Null until the first read. */
    val rows: StateFlow<List<PayeeRow>?> =
        combine(payeeRepo.observeAllActive(), transactions.observeAllActive()) { people, all ->
            val byPayee = all.groupBy { it.payeeId }
            people.map { PayeeRow(it, PayeeLedger.of(byPayee[it.id].orEmpty())) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(name: String, phone: String?, upiId: String?) {
        viewModelScope.launch {
            payeeRepo.upsert(
                PayeeEntity(name = name, avatarUri = null, phone = phone, upiId = upiId, createdAt = System.currentTimeMillis())
            )
        }
    }
}

data class PayeeDetailState(
    val today: LocalDate,
    val payee: Payee,
    val ledger: PayeeLedger,
    /** The rows behind the ledger, for their notes. */
    val transactions: Map<Long, TransactionEntity>,
    val accounts: List<AccountBalance>
)

@HiltViewModel
class PayeeDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val payeeRepo: PayeeRepo,
    transactions: TransactionRepo,
    accounts: AccountRepo,
    private val recordSettlement: RecordSettlement,
    private val preferenceStore: AppPreferencesStore
) : ViewModel() {

    val whatsappBold: StateFlow<Boolean> = preferenceStore.preferencesFlow
        .map { it.whatsappBold }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setWhatsappBold(enabled: Boolean) {
        viewModelScope.launch { preferenceStore.updateWhatsappBold(enabled) }
    }

    private val payeeId: Long = savedState.get<Long>(PAYEE_ID_ARG) ?: 0L
    private val today = LocalDate.now()

    val state: StateFlow<PayeeDetailState?> = combine(
        payeeRepo.observeById(payeeId),
        transactions.observeForPayee(payeeId),
        accounts.observeBalances()
    ) { payee, rows, balances ->
        payee?.let {
            PayeeDetailState(today, it, PayeeLedger.of(rows), rows.associateBy { row -> row.id }, balances)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(name: String, phone: String?, upiId: String?) {
        val payee = state.value?.payee ?: return
        viewModelScope.launch { payeeRepo.update(payee.toEntity(name, phone, upiId)) }
    }

    /** Only a settled person can be removed, so no balance drops out of the totals unnoticed. */
    fun remove(onRemoved: () -> Unit) {
        val current = state.value ?: return
        if (current.ledger.net != 0L) return
        viewModelScope.launch {
            payeeRepo.softDelete(payeeId)
            onRemoved()
        }
    }

    fun settle(theyPaidYou: Boolean, amount: Long, date: LocalDate, accountId: Long?, note: String, onSaved: () -> Unit = {}) {
        if (amount <= 0) return
        viewModelScope.launch {
            val at = date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            recordSettlement(payeeId, theyPaidYou, amount, at, accountId, note)
            onSaved()
        }
    }
}

/** Whether a ledger row adds to what the person owes the user. */
fun addsToTheirDebt(type: TxnType): Boolean = type == TxnType.DUE_TO || type == TxnType.SETTLEMENT_TO
