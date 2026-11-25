package com.saadm.zenith.ui

import androidx.lifecycle.SavedStateHandle
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.testing.TestGraph
import com.saadm.zenith.testing.awaitUntil
import com.saadm.zenith.ui.navigation.TXN_ID_ARG
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What each screen's ViewModel writes, against a real database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelTest {

    private val graph = TestGraph()
    private val food = 3L

    @After
    fun tearDown() = graph.close()

    private fun rows() = runBlocking { graph.transactions.observeAllActive().first() }

    private fun row(type: TxnType, payeeId: Long? = null) = TransactionEntity(
        amount = 500, type = type, transactedAt = System.currentTimeMillis(), createdAt = 1, updatedAt = 1,
        categoryId = food, payeeId = payeeId, accountId = null, note = null, receiptUri = null
    )

    private fun person() = runBlocking {
        graph.payees.upsert(PayeeEntity(name = "Dev", avatarUri = null, phone = null, upiId = null, createdAt = 1))
    }

    @Test
    fun addSavesUnderTheChosenDefaultCategory() {
        runBlocking { graph.preferences.updateDefaultCategory(TxnType.EXPENSE, food) }
        val vm = graph.add()
        vm.change { it.copy(amount = "40+2") }
        vm.save()
        awaitUntil { vm.saved.value != null }
        assertEquals(listOf(4_200L to food), rows().map { it.amount to it.categoryId })
    }

    @Test
    fun editChangesTheRowInsteadOfAddingOne() {
        runBlocking { graph.transactions.save(row(TxnType.EXPENSE)) }
        val id = rows().single().id
        val vm = graph.add(SavedStateHandle(mapOf(TXN_ID_ARG to id)))
        awaitUntil { vm.form.value.amount.isNotEmpty() }
        vm.change { it.copy(amount = "9") }
        vm.save()
        awaitUntil { vm.saved.value != null }
        assertEquals(listOf(id to 900L), rows().map { it.id to it.amount })
    }

    @Test
    fun splitWritesTheExpenseAndWhatEachPersonOwes() {
        val dev = person()
        val vm = graph.add()
        vm.change { it.copy(amount = "10", people = listOf(dev)) }
        vm.saveSplit()
        awaitUntil { vm.saved.value != null }
        assertEquals(
            listOf(TxnType.DUE_TO to 500L, TxnType.EXPENSE to 1_000L),
            rows().map { it.type to it.amount }.sortedBy { it.second }
        )
    }

    @Test
    fun deletingFromTheListRemovesTheEntry() {
        runBlocking { graph.transactions.save(row(TxnType.EXPENSE)) }
        val vm = graph.txnList()
        graph.watch(vm.state)
        awaitUntil { vm.state.value?.count == 1 }
        vm.delete(vm.state.value!!.days.single().items.single())
        awaitUntil { vm.state.value?.count == 0 }
        assertEquals(emptyList<TransactionEntity>(), rows())
    }

    @Test
    fun settlingUpClearsTheBalance() {
        val dev = person()
        runBlocking { graph.transactions.save(row(TxnType.DUE_TO, dev)) }
        val vm = graph.payee(dev)
        graph.watch(vm.state)
        awaitUntil { vm.state.value?.ledger?.net == 500L }
        vm.settle(true, 500, vm.state.value!!.today, null, "")
        awaitUntil { vm.state.value?.ledger?.net == 0L }
    }

    @Test
    fun savingAndRemovingABudget() {
        val vm = graph.budgets()
        graph.watch(vm.state)
        awaitUntil { vm.state.value != null }
        vm.save(food, 100_000)
        awaitUntil { vm.state.value?.budgets?.categories?.size == 1 }
        assertEquals(100_000L, vm.state.value!!.budgets.categories.single().limit)
        vm.remove(food)
        awaitUntil { vm.state.value?.budgets?.categories?.isEmpty() == true }
    }

    @Test
    fun addingACategoryShowsIt() {
        val vm = graph.categoriesViewModel()
        graph.watch(vm.state)
        awaitUntil { vm.state.value != null }
        vm.add("Books", "EDUCATION")
        awaitUntil { vm.state.value?.rows?.any { it.category.name == "Books" } == true }
    }
}
