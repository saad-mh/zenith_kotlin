package com.saadm.zenith.testing

import android.os.Looper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.data.preferences.AppPreferencesStore
import com.saadm.zenith.data.preferences.SettingsRepository
import com.saadm.zenith.data.repository.AccountRepoImpl
import com.saadm.zenith.data.repository.BudgetRepoImpl
import com.saadm.zenith.data.repository.CategoryRepoImpl
import com.saadm.zenith.data.repository.PayeeRepoImpl
import com.saadm.zenith.data.repository.TransactionRepoImpl
import com.saadm.zenith.domain.usecase.CopyBudgetsFromPreviousMonth
import com.saadm.zenith.domain.usecase.HideBudget
import com.saadm.zenith.domain.usecase.ObserveMonthBudgets
import com.saadm.zenith.domain.usecase.ObserveTxnNames
import com.saadm.zenith.domain.usecase.RecordSettlement
import com.saadm.zenith.domain.usecase.SaveBudget
import com.saadm.zenith.domain.usecase.SearchTransactions
import com.saadm.zenith.domain.usecase.SuggestBudgets
import com.saadm.zenith.ui.add.AddDraft
import com.saadm.zenith.ui.add.AddTxnViewModel
import com.saadm.zenith.ui.budgets.BudgetsViewModel
import com.saadm.zenith.ui.categories.CategoriesViewModel
import com.saadm.zenith.ui.navigation.PAYEE_ID_ARG
import com.saadm.zenith.ui.people.PayeeDetailViewModel
import com.saadm.zenith.ui.transactions.TxnListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.util.UUID

/** The real repositories and use cases over an in-memory database, wired by hand. */
class TestGraph {
    private val context = RuntimeEnvironment.getApplication()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")

    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .addCallback(AppDatabase.DB_CREATE_CALLBACK)
        .build()
    val preferences = AppPreferencesStore(PreferenceDataStoreFactory.create(scope = scope) { file })
    private val settings = SettingsRepository(preferences)
    val accounts = AccountRepoImpl(db.accountDao())
    val categories = CategoryRepoImpl(db.categoryDao())
    val transactions = TransactionRepoImpl(db.transactionDao())
    val payees = PayeeRepoImpl(db.payeeDao(), db.payeeBalanceDao(), db.transactionDao())
    private val budgets = BudgetRepoImpl(db.budgetDao())
    private val names = ObserveTxnNames(categories, payees, accounts)

    fun add(state: SavedStateHandle = SavedStateHandle()) =
        AddTxnViewModel(state, transactions, categories, payees, accounts, settings, AddDraft(), preferences)

    fun txnList() = TxnListViewModel(transactions, settings, names, SearchTransactions(transactions))

    fun payee(id: Long) = PayeeDetailViewModel(
        SavedStateHandle(mapOf(PAYEE_ID_ARG to id)), payees, transactions, accounts,
        RecordSettlement(transactions, categories), preferences
    )

    fun budgets() = BudgetsViewModel(
        settings, budgets, categories, ObserveMonthBudgets(budgets, transactions), SuggestBudgets(transactions),
        SaveBudget(budgets), HideBudget(budgets), CopyBudgetsFromPreviousMonth(budgets)
    )

    fun categoriesViewModel() = CategoriesViewModel(categories, transactions, settings)

    /** Keeps a state flow collecting, as a screen would. */
    fun <T> watch(flow: Flow<T>) {
        scope.launch { flow.collect {} }
    }

    fun close() {
        scope.cancel()
        db.close()
    }
}

/** Runs the main looper until [condition] holds, since writes finish on Room's own threads. */
fun awaitUntil(condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + 10_000
    while (true) {
        shadowOf(Looper.getMainLooper()).idle()
        if (condition()) return
        check(System.currentTimeMillis() < deadline) { "Timed out waiting for the condition" }
        Thread.sleep(10)
    }
}
