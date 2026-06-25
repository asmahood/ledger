package io.github.asmahood.ledger.ui.overview

import app.cash.turbine.test
import io.github.asmahood.ledger.data.projection.PeriodTotals
import io.github.asmahood.ledger.data.repository.FakeTransactionRepository
import io.github.asmahood.ledger.rule.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class OverviewViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeTransactionRepository()

    @Test
    fun overviewViewModel_initialState_isLoading() {
        val viewModel = OverviewViewModel(repository)

        // No collector yet, so the WhileSubscribed stream is cold: value is the initial Loading.
        assertEquals(OverviewUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun overviewViewModel_defaultPeriod_isThisMonth() {
        val viewModel = OverviewViewModel(repository)

        assertEquals(OverviewPeriod.THIS_MONTH, viewModel.selectedPeriod.value)
    }

    @Test
    fun overviewViewModel_repoEmitsTotals_mapsToSummary() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 4000.0, expenses = 3000.0))

        val viewModel = OverviewViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Success
            assertEquals(4000.0, state.summary.income, 0.0)
            assertEquals(3000.0, state.summary.expenses, 0.0)
            assertEquals(1000.0, state.summary.saved, 0.0)
            assertEquals(75, state.summary.expensesPercentOfIncome)
            assertEquals(25, state.summary.savedPercentOfIncome)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_onSubscribe_queriesCurrentMonthRange() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))

        val viewModel = OverviewViewModel(repository)

        viewModel.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val expected = OverviewPeriod.THIS_MONTH.toDateRange(LocalDate.now())
        assertTrue(
            "Expected current-month range to be queried",
            repository.periodTotalsRequestedFor.contains(expected.start to expected.endInclusive),
        )
    }

    @Test
    fun overviewViewModel_selectingPeriod_queriesThatPeriodsRange() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))

        val viewModel = OverviewViewModel(repository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onPeriodSelected(OverviewPeriod.THREE_MONTHS)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(OverviewPeriod.THREE_MONTHS, viewModel.selectedPeriod.value)
        val expected = OverviewPeriod.THREE_MONTHS.toDateRange(LocalDate.now())
        assertTrue(
            "Expected the newly selected period's range to be queried",
            repository.periodTotalsRequestedFor.contains(expected.start to expected.endInclusive),
        )
    }

    @Test
    fun overviewViewModel_repoThrows_emitsError() = runTest {
        repository.streamError = RuntimeException("boom")

        val viewModel = OverviewViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Error
            assertEquals("boom", state.message)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
