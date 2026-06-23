package io.github.asmahood.ledger.ui.transaction.form

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
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
import java.time.LocalDate
import java.util.Locale

class TransactionFormViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeTransactionRepository()
    private val categoryRepository = FakeCategoryRepository()

    private val expenseCategory =
        Category(id = 1L, name = "Groceries", type = TransactionType.EXPENSE)
    private val incomeCategory =
        Category(id = 2L, name = "Salary", type = TransactionType.INCOME)

    // Add-mode ViewModel (no transactionId in SavedStateHandle).
    private fun viewModel() =
        TransactionFormViewModel(repository, categoryRepository, SavedStateHandle())

    // Creates an edit-mode ViewModel pre-loaded with an existing transaction.
    private fun editViewModel(transaction: Transaction): TransactionFormViewModel {
        repository.setTransactions(listOf(transaction))
        return TransactionFormViewModel(
            repository,
            categoryRepository,
            SavedStateHandle(mapOf("transactionId" to transaction.id))
        )
    }

    private fun sampleTransaction(
        id: Long = 5L,
        amount: Double = 42.50,
        notes: String? = "weekly shop",
    ) = Transaction(
        id = id,
        amount = amount,
        date = LocalDate.of(2026, 1, 15),
        vendor = "Food Basics",
        type = TransactionType.EXPENSE,
        notes = notes,
        category = expenseCategory,
    )

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

    // ── Edit mode: pre-population ─────────────────────────────────────────────────

    @Test
    fun editMode_initialState_prePopulatesFromRepository() {
        val vm = editViewModel(sampleTransaction())

        val state = vm.uiState.value
        assertEquals(5L, state.id)
        assertTrue(state.isEditMode)
        assertEquals("42.50", state.amount)
        assertEquals("01/15/2026", state.date)
        assertEquals("Food Basics", state.vendor)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(expenseCategory, state.category)
        assertEquals("weekly shop", state.notes)
    }

    @Test
    fun addMode_initialState_isNotEditMode() {
        assertFalse(viewModel().uiState.value.isEditMode)
    }

    @Test
    fun editMode_initialState_amountFormattedToTwoDecimals() {
        val vm = editViewModel(sampleTransaction(amount = 7.0))

        assertEquals("7.00", vm.uiState.value.amount)
    }

    @Test
    fun editMode_initialState_amountUsesDotDecimalSeparatorInCommaLocale() {
        // The pre-populated amount is parsed back via amount.toDouble(), which only accepts '.';
        // formatting must not honour a comma-decimal default locale (e.g. Germany).
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            val vm = editViewModel(sampleTransaction(amount = 42.5))

            assertEquals("42.50", vm.uiState.value.amount)
            assertTrue(vm.uiState.value.isFormValid)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun editMode_initialState_nullNotes_mapsToEmptyString() {
        val vm = editViewModel(sampleTransaction(notes = null))

        assertEquals("", vm.uiState.value.notes)
    }

    @Test
    fun editMode_transactionNotFound_emitsDismissed() = runTest {
        // Transaction absent from repo — simulates deletion between navigation and ViewModel creation.
        val vm = TransactionFormViewModel(
            repository,
            categoryRepository,
            SavedStateHandle(mapOf("transactionId" to 99L))
        )

        vm.events.test {
            assertEquals(TransactionFormEvent.Dismissed, awaitItem())
        }
    }

    // ── Edit mode: save updates ───────────────────────────────────────────────────

    @Test
    fun editMode_save_callsUpdateNotInsert() = runTest {
        val vm = editViewModel(sampleTransaction())

        vm.events.test {
            vm.saveTransaction()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(repository.updated.isNotEmpty())
        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun editMode_save_updatesWithCorrectId() = runTest {
        val vm = editViewModel(sampleTransaction())
        vm.updateVendor("No Frills")

        vm.events.test {
            vm.saveTransaction()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val updated = repository.updated.single()
        assertEquals(5L, updated.id)
        assertEquals("No Frills", updated.vendor)
    }

    @Test
    fun editMode_save_emitsSavedSuccessfully() = runTest {
        val vm = editViewModel(sampleTransaction())

        vm.events.test {
            vm.saveTransaction()
            assertEquals(TransactionFormEvent.SavedSuccessfully, awaitItem())
        }
    }

    @Test
    fun editMode_save_whenUpdateFails_emitsShowError() = runTest {
        repository.updateError = RuntimeException("disk full")
        val vm = editViewModel(sampleTransaction())

        vm.events.test {
            vm.saveTransaction()

            assertTrue(awaitItem() is TransactionFormEvent.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────────

    @Test
    fun deleteTransaction_emitsSavedSuccessfully() = runTest {
        val vm = editViewModel(sampleTransaction())

        vm.events.test {
            vm.deleteTransaction()
            assertEquals(TransactionFormEvent.SavedSuccessfully, awaitItem())
        }
    }

    @Test
    fun deleteTransaction_callsRepositoryDeleteWithCorrectId() = runTest {
        val vm = editViewModel(sampleTransaction())

        vm.events.test {
            vm.deleteTransaction()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, repository.deleted.size)
        assertEquals(5L, repository.deleted.single().id)
    }

    @Test
    fun deleteTransaction_whenDeleteFails_emitsShowError() = runTest {
        repository.deleteError = RuntimeException("FK constraint")
        val vm = editViewModel(sampleTransaction())

        vm.events.test {
            vm.deleteTransaction()
            assertTrue(awaitItem() is TransactionFormEvent.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
