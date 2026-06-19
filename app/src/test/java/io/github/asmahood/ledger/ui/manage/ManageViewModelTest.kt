package io.github.asmahood.ledger.ui.manage

import app.cash.turbine.test
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.FakeCategoryRepository
import io.github.asmahood.ledger.rule.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ManageViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeCategoryRepository()

    @Test
    fun manageViewModel_initialState_isLoading() {
        val viewModel = ManageViewModel(repository)

        // No collector yet, so the WhileSubscribed stream is cold: value is the initial Loading.
        assertEquals(ManageUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun manageViewModel_repoEmitsMixedList_partitionsByType() = runTest {
        val groceries = Category(id = 1L, name = "Groceries", type = TransactionType.EXPENSE)
        val rent = Category(id = 2L, name = "Rent", type = TransactionType.EXPENSE)
        val salary = Category(id = 3L, name = "Salary", type = TransactionType.INCOME)
        repository.setCategories(listOf(groceries, rent, salary))

        val viewModel = ManageViewModel(repository)

        // StateFlow conflates: the first item is the settled value, not the initial Loading
        // (the Loading initial state is covered separately above via .value).
        viewModel.uiState.test {
            val state = awaitItem() as ManageUiState.Success
            assertEquals(listOf(groceries, rent), state.expenseCategories)
            assertEquals(listOf(salary), state.incomeCategories)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun manageViewModel_repoEmitsEmpty_successWithEmptyLists() = runTest {
        repository.setCategories(emptyList())

        val viewModel = ManageViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem() as ManageUiState.Success
            assertTrue(state.expenseCategories.isEmpty())
            assertTrue(state.incomeCategories.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun manageViewModel_repoThrows_emitsError() = runTest {
        repository.streamError = RuntimeException("boom")

        val viewModel = ManageViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem() as ManageUiState.Error
            assertEquals("boom", state.message)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
