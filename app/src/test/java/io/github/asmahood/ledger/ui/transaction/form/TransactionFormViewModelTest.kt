package io.github.asmahood.ledger.ui.transaction.form

import app.cash.turbine.test
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.FakeCategoryRepository
import io.github.asmahood.ledger.data.repository.FakeTransactionRepository
import io.github.asmahood.ledger.rule.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionFormViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeTransactionRepository()
    private val categoryRepository = FakeCategoryRepository()

    private val expenseCategory =
        Category(id = 1L, name = "Groceries", type = TransactionType.EXPENSE)
    private val incomeCategory =
        Category(id = 2L, name = "Salary", type = TransactionType.INCOME)

    private fun viewModel() = TransactionFormViewModel(repository, categoryRepository)

    /** Drives [viewModel] into a fully valid expense state ready to save. */
    private fun TransactionFormViewModel.fillValidExpense() {
        updateAmount("12.50")
        updateVendor("Food Basics")
        updateCategory(expenseCategory)
    }

    // ── Initial state ─────────────────────────────────────────────────────────────

    @Test
    fun initialState_isEmptyAndInvalid() {
        val state = viewModel().uiState.value

        assertEquals("", state.amount)
        assertEquals("", state.vendor)
        assertEquals("", state.notes)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertNull(state.category)
        assertFalse(state.isFormValid)
    }

    @Test
    fun init_collectsCategoriesFromRepository() {
        categoryRepository.setCategories(listOf(expenseCategory, incomeCategory))

        assertEquals(
            listOf(expenseCategory, incomeCategory),
            viewModel().uiState.value.categories,
        )
    }

    // ── Field updates ─────────────────────────────────────────────────────────────

    @Test
    fun updateAmount_updatesStateAndValidity() {
        val vm = viewModel()
        vm.updateVendor("Food Basics")
        vm.updateCategory(expenseCategory)

        vm.updateAmount("9.99")

        assertEquals("9.99", vm.uiState.value.amount)
        assertTrue(vm.uiState.value.isFormValid)
    }

    @Test
    fun updateVendor_updatesState() {
        val vm = viewModel()
        vm.updateVendor("Hydro One")
        assertEquals("Hydro One", vm.uiState.value.vendor)
    }

    @Test
    fun updateNotes_updatesState() {
        val vm = viewModel()
        vm.updateNotes("monthly bill")
        assertEquals("monthly bill", vm.uiState.value.notes)
    }

    @Test
    fun updateDate_updatesState() {
        val vm = viewModel()
        vm.updateDate("01/15/2026")
        assertEquals("01/15/2026", vm.uiState.value.date)
    }

    @Test
    fun updateCategory_updatesState() {
        val vm = viewModel()
        vm.updateCategory(expenseCategory)
        assertEquals(expenseCategory, vm.uiState.value.category)
    }

    // ── Type toggle resets category ───────────────────────────────────────────────

    @Test
    fun updateType_switchingType_clearsMismatchedCategory() {
        val vm = viewModel()
        vm.updateCategory(expenseCategory)

        vm.updateType(TransactionType.INCOME)

        assertEquals(TransactionType.INCOME, vm.uiState.value.type)
        assertNull(vm.uiState.value.category)
    }

    @Test
    fun updateType_sameType_keepsCategory() {
        val vm = viewModel()
        vm.updateCategory(expenseCategory)

        vm.updateType(TransactionType.EXPENSE)

        assertEquals(expenseCategory, vm.uiState.value.category)
    }

    // ── Save: blocked when invalid ────────────────────────────────────────────────

    @Test
    fun saveTransaction_whenInvalid_doesNotInsertOrEmit() = runTest {
        val vm = viewModel()

        vm.events.test {
            vm.saveTransaction()
            expectNoEvents()
        }

        assertTrue(repository.inserted.isEmpty())
    }

    // ── Save: success ─────────────────────────────────────────────────────────────

    @Test
    fun saveTransaction_whenValid_emitsSavedSuccessfully() = runTest {
        val vm = viewModel()
        vm.fillValidExpense()

        vm.events.test {
            vm.saveTransaction()
            assertEquals(TransactionFormEvent.SavedSuccessfully, awaitItem())
        }
    }

    @Test
    fun saveTransaction_whenValid_insertsMappedTransaction() = runTest {
        val vm = viewModel()
        vm.fillValidExpense()

        vm.events.test {
            vm.saveTransaction()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val inserted = repository.inserted.single()
        assertEquals(0L, inserted.id)
        assertEquals(12.50, inserted.amount, 0.0)
        assertEquals("Food Basics", inserted.vendor)
        assertEquals(TransactionType.EXPENSE, inserted.type)
        assertEquals(expenseCategory, inserted.category)
    }

    // ── Save: error path ──────────────────────────────────────────────────────────

    @Test
    fun saveTransaction_whenInsertFails_emitsShowError() = runTest {
        repository.insertError = RuntimeException("disk full")
        val vm = viewModel()
        vm.fillValidExpense()

        vm.events.test {
            vm.saveTransaction()

            val event = awaitItem()
            assertTrue(event is TransactionFormEvent.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
