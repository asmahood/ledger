package io.github.asmahood.ledger.ui.transaction.list

import app.cash.turbine.test
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.FakeTransactionRepository
import io.github.asmahood.ledger.rule.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class TransactionListViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeTransactionRepository()

    private val groceries = Category(id = 1L, name = "Groceries", type = TransactionType.EXPENSE)
    private val salary = Category(id = 2L, name = "Salary", type = TransactionType.INCOME)

    private val mar12 = LocalDate.of(2026, 3, 12)
    private val mar11 = LocalDate.of(2026, 3, 11)

    private fun expense(id: Long, date: LocalDate) =
        Transaction(id, 10.0, date, "Vendor $id", TransactionType.EXPENSE, null, groceries)

    private fun income(id: Long, date: LocalDate) =
        Transaction(id, 1500.0, date, "Employer", TransactionType.INCOME, null, salary)

    @Test
    fun transactionListViewModel_initialState_isLoading() {
        val viewModel = TransactionListViewModel(repository)

        // No collector yet, so the WhileSubscribed stream is cold: value is the initial Loading.
        assertEquals(TransactionListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun transactionListViewModel_repoEmitsTransactions_groupsByDay() = runTest {
        val first = income(0L, mar12)
        val second = expense(1L, mar12)
        val third = expense(2L, mar11)
        // Ordered newest-first, mirroring the DAO's `ORDER BY date DESC`.
        repository.setTransactions(listOf(first, second, third))

        val viewModel = TransactionListViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem() as TransactionListUiState.Success

            // One group per day, preserving the newest-first order of the source list.
            assertEquals(listOf(mar12, mar11), state.groups.map { it.date })
            // Same-day transactions stay together, in source order.
            assertEquals(listOf(first, second), state.groups[0].transactions)
            assertEquals(listOf(third), state.groups[1].transactions)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun transactionListViewModel_repoEmitsEmpty_successWithNoGroups() = runTest {
        repository.setTransactions(emptyList())

        val viewModel = TransactionListViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem() as TransactionListUiState.Success
            assertTrue(state.groups.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun transactionListViewModel_repoThrows_emitsError() = runTest {
        repository.streamError = RuntimeException("boom")

        val viewModel = TransactionListViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem() as TransactionListUiState.Error
            assertEquals("boom", state.message)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
