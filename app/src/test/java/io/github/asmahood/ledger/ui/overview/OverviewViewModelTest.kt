package io.github.asmahood.ledger.ui.overview

import app.cash.turbine.test
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.projection.CategoryMonthSpend
import io.github.asmahood.ledger.data.projection.MonthlyTotal
import io.github.asmahood.ledger.data.projection.PeriodTotals
import io.github.asmahood.ledger.data.repository.FakeCategoryRepository
import io.github.asmahood.ledger.data.repository.FakeTransactionRepository
import io.github.asmahood.ledger.rule.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class OverviewViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeTransactionRepository()
    private val categoryRepository = FakeCategoryRepository()

    @Test
    fun overviewViewModel_initialState_isLoading() {
        val viewModel = OverviewViewModel(repository, categoryRepository)

        // No collector yet, so the WhileSubscribed stream is cold: value is the initial Loading.
        assertEquals(OverviewUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun overviewViewModel_defaultPeriod_isThisMonth() {
        val viewModel = OverviewViewModel(repository, categoryRepository)

        assertEquals(OverviewPeriod.THIS_MONTH, viewModel.selectedPeriod.value)
    }

    @Test
    fun overviewViewModel_repoEmitsTotals_mapsToSummary() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 4000.0, expenses = 3000.0))

        val viewModel = OverviewViewModel(repository, categoryRepository)

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

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val expected = OverviewPeriod.THIS_MONTH.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected current-month range to be queried",
            repository.periodTotalsRequestedFor.contains(expected.start to expected.endInclusive),
        )
    }

    @Test
    fun overviewViewModel_selectingPeriod_queriesThatPeriodsRange() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onPeriodSelected(OverviewPeriod.THREE_MONTHS)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(OverviewPeriod.THREE_MONTHS, viewModel.selectedPeriod.value)
        val expected = OverviewPeriod.THREE_MONTHS.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected the newly selected period's range to be queried",
            repository.periodTotalsRequestedFor.contains(expected.start to expected.endInclusive),
        )
    }

    @Test
    fun overviewViewModel_repoThrows_emitsError() = runTest {
        repository.streamError = RuntimeException("boom")

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Error
            assertEquals("boom", state.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Category spend chart ──────────────────────────────────────────────────────

    private fun spend(categoryId: Long, categoryName: String) =
        CategoryMonthSpend(categoryId = categoryId, categoryName = categoryName, year = 2026, month = 1, total = 100.0)

    @Test
    fun overviewViewModel_categorySpend_mapsToVisibleSortedSeries() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setCategoryMonthlyTotals(
            listOf(spend(2, "Rent"), spend(1, "Gas")),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Success
            assertEquals(listOf("Gas", "Rent"), state.categorySpendChart.series.map { it.categoryName })
            assertTrue(state.categorySpendChart.series.all { it.isVisible })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_onCategoryToggled_hidesThenShowsThatCategory() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setCategoryMonthlyTotals(
            listOf(spend(1, "Gas"), spend(2, "Rent")),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem() // all visible

            viewModel.onCategoryToggled(1)
            val hidden = awaitItem() as OverviewUiState.Success
            assertFalse(hidden.categorySpendChart.series.first { it.categoryId == 1L }.isVisible)
            // Toggling one category leaves the others untouched.
            assertTrue(hidden.categorySpendChart.series.first { it.categoryId == 2L }.isVisible)

            viewModel.onCategoryToggled(1)
            val shown = awaitItem() as OverviewUiState.Success
            assertTrue(shown.categorySpendChart.series.first { it.categoryId == 1L }.isVisible)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_selectingPeriod_clearsHiddenCategories() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setCategoryMonthlyTotals(listOf(spend(1, "Gas")))

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()

            viewModel.onCategoryToggled(1)
            val hidden = awaitItem() as OverviewUiState.Success
            assertFalse(hidden.categorySpendChart.series.first { it.categoryId == 1L }.isVisible)

            // Switching period must not carry the hidden filter over to the new period.
            viewModel.onPeriodSelected(OverviewPeriod.THREE_MONTHS)
            val afterSwitch = awaitItem() as OverviewUiState.Success
            assertTrue(afterSwitch.categorySpendChart.series.first { it.categoryId == 1L }.isVisible)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_onSubscribe_queriesCategoryTotalsForCurrentMonthRange() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val expected = OverviewPeriod.THIS_MONTH.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected category totals to be queried for the current-month range",
            repository.categoryMonthlyTotalsRequestedFor.contains(expected.start to expected.endInclusive),
        )
    }

    // ── Total income chart ────────────────────────────────────────────────────────

    @Test
    fun overviewViewModel_totalIncome_mapsToChartAmounts() = runTest {
        val today = LocalDate.now()
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        // Distinct income/expense totals so a stream mix-up (income wired to the expense chart or
        // vice versa) is detectable rather than masked by identical values.
        repository.setMonthlyTotalsByType(
            TransactionType.INCOME,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 4000.0)),
        )
        repository.setMonthlyTotalsByType(
            TransactionType.EXPENSE,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 3000.0)),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Success
            assertTrue(state.totalIncomeChart.amounts.contains(4000.0))
            assertFalse(state.totalIncomeChart.amounts.contains(3000.0))
            assertEquals(state.totalIncomeChart.monthLabels.size, state.totalIncomeChart.amounts.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_onSubscribe_queriesIncomeTotalsForCurrentMonthRangeAndType() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val expected = OverviewPeriod.THIS_MONTH.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected income totals to be queried for the current-month range",
            repository.monthlyTotalsByTypeRequestedFor.contains(
                Triple(TransactionType.INCOME, expected.start, expected.endInclusive),
            ),
        )
    }

    @Test
    fun overviewViewModel_selectingPeriod_reDerivesTotalIncomeChart() = runTest {
        val today = LocalDate.now()
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setMonthlyTotalsByType(
            TransactionType.INCOME,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 4000.0)),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val initial = awaitItem() as OverviewUiState.Success
            assertTrue(initial.totalIncomeChart.amounts.contains(4000.0))

            viewModel.onPeriodSelected(OverviewPeriod.THREE_MONTHS)
            cancelAndIgnoreRemainingEvents()
        }

        // AC3: switching periods must re-query the income stream for the newly selected range.
        val expected = OverviewPeriod.THREE_MONTHS.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected the newly selected period's range to be queried for income totals",
            repository.monthlyTotalsByTypeRequestedFor.contains(
                Triple(TransactionType.INCOME, expected.start, expected.endInclusive),
            ),
        )
    }

    // ── Total expense chart ───────────────────────────────────────────────────────

    @Test
    fun overviewViewModel_totalExpense_mapsToChartAmounts() = runTest {
        val today = LocalDate.now()
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        // Distinct income/expense totals so a stream mix-up is detectable rather than masked by
        // identical values.
        repository.setMonthlyTotalsByType(
            TransactionType.INCOME,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 4000.0)),
        )
        repository.setMonthlyTotalsByType(
            TransactionType.EXPENSE,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 3000.0)),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Success
            assertTrue(state.totalExpenseChart.amounts.contains(3000.0))
            assertFalse(state.totalExpenseChart.amounts.contains(4000.0))
            assertEquals(state.totalExpenseChart.monthLabels.size, state.totalExpenseChart.amounts.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_onSubscribe_queriesExpenseTotalsForCurrentMonthRangeAndType() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val expected = OverviewPeriod.THIS_MONTH.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected expense totals to be queried for the current-month range",
            repository.monthlyTotalsByTypeRequestedFor.contains(
                Triple(TransactionType.EXPENSE, expected.start, expected.endInclusive),
            ),
        )
    }

    @Test
    fun overviewViewModel_selectingPeriod_reDerivesTotalExpenseChart() = runTest {
        val today = LocalDate.now()
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setMonthlyTotalsByType(
            TransactionType.EXPENSE,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 3000.0)),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val initial = awaitItem() as OverviewUiState.Success
            assertTrue(initial.totalExpenseChart.amounts.contains(3000.0))

            viewModel.onPeriodSelected(OverviewPeriod.THREE_MONTHS)
            cancelAndIgnoreRemainingEvents()
        }

        // Switching periods must re-query the expense stream for the newly selected range.
        val expected = OverviewPeriod.THREE_MONTHS.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected the newly selected period's range to be queried for expense totals",
            repository.monthlyTotalsByTypeRequestedFor.contains(
                Triple(TransactionType.EXPENSE, expected.start, expected.endInclusive),
            ),
        )
    }

    // ── Total savings chart ───────────────────────────────────────────────────────

    @Test
    fun overviewViewModel_totalSavings_mapsToNetOfIncomeAndExpense() = runTest {
        val today = LocalDate.now()
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setMonthlyTotalsByType(
            TransactionType.INCOME,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 4000.0)),
        )
        repository.setMonthlyTotalsByType(
            TransactionType.EXPENSE,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 3000.0)),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Success
            // Net savings is income − expense: 4000 − 3000 = 1000 for the current month.
            assertTrue(state.totalSavingsChart.amounts.contains(1000.0))
            assertEquals(state.totalSavingsChart.monthLabels.size, state.totalSavingsChart.amounts.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_totalSavings_expenseExceedsIncome_isNegative() = runTest {
        val today = LocalDate.now()
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setMonthlyTotalsByType(
            TransactionType.INCOME,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 3000.0)),
        )
        repository.setMonthlyTotalsByType(
            TransactionType.EXPENSE,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 4000.0)),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Success
            // 3000 − 4000 = −1000: overspent months net below zero.
            assertTrue(state.totalSavingsChart.amounts.contains(-1000.0))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_selectingPeriod_reDerivesTotalSavingsChart() = runTest {
        val today = LocalDate.now()
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        repository.setMonthlyTotalsByType(
            TransactionType.INCOME,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 4000.0)),
        )
        repository.setMonthlyTotalsByType(
            TransactionType.EXPENSE,
            listOf(MonthlyTotal(year = today.year, month = today.monthValue, total = 3000.0)),
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val initial = awaitItem() as OverviewUiState.Success
            assertTrue(initial.totalSavingsChart.amounts.contains(1000.0))

            viewModel.onPeriodSelected(OverviewPeriod.THREE_MONTHS)
            cancelAndIgnoreRemainingEvents()
        }

        // Savings is derived from the income and expense streams, so a period switch must re-query
        // both for the newly selected range.
        val expected = OverviewPeriod.THREE_MONTHS.toDateRange(LocalDate.now(), earliest = null)
        assertTrue(
            "Expected income totals to be re-queried for the new range",
            repository.monthlyTotalsByTypeRequestedFor.contains(
                Triple(TransactionType.INCOME, expected.start, expected.endInclusive),
            ),
        )
        assertTrue(
            "Expected expense totals to be re-queried for the new range",
            repository.monthlyTotalsByTypeRequestedFor.contains(
                Triple(TransactionType.EXPENSE, expected.start, expected.endInclusive),
            ),
        )
    }

    @Test
    fun overviewViewModel_allPeriod_queriesFromEarliestTransaction() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 0.0, expenses = 0.0))
        val earliest = LocalDate.of(2024, 3, 17)
        repository.setEarliestTransactionDate(earliest)

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onPeriodSelected(OverviewPeriod.ALL)
            cancelAndIgnoreRemainingEvents()
        }

        val expected = OverviewPeriod.ALL.toDateRange(LocalDate.now(), earliest)
        assertEquals(earliest, expected.start)
        assertTrue(
            "Expected the ALL range to start at the earliest transaction",
            repository.periodTotalsRequestedFor.contains(expected.start to expected.endInclusive),
        )
    }

    @Test
    fun overviewViewModel_budgetSummary_scalesTargetsToSelectedPeriod() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 4000.0, expenses = 3000.0))
        categoryRepository.setCategories(
            listOf(
                Category(id = 1, name = "Groceries", type = TransactionType.EXPENSE, budget = 400.0),
                Category(id = 9, name = "Salary", type = TransactionType.INCOME, budget = 4000.0),
            )
        )
        repository.setCategoryMonthlyTotals(
            listOf(
                CategoryMonthSpend(
                    categoryId = 1, categoryName = "Groceries",
                    year = LocalDate.now().year, month = LocalDate.now().monthValue, total = 310.0,
                )
            )
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val thisMonth = awaitItem() as OverviewUiState.Success
            assertEquals(400.0, thisMonth.budgetSummary.budgeted.single().target, 0.0)

            viewModel.onPeriodSelected(OverviewPeriod.THREE_MONTHS)

            val threeMonths = expectMostRecentItem() as OverviewUiState.Success
            assertEquals(1200.0, threeMonths.budgetSummary.budgeted.single().target, 0.0)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overviewViewModel_categoryWithoutBudget_landsInUnbudgeted() = runTest {
        repository.setPeriodTotals(PeriodTotals(income = 4000.0, expenses = 3000.0))
        categoryRepository.setCategories(
            listOf(
                Category(id = 9, name = "Salary", type = TransactionType.INCOME, budget = 4000.0),
                Category(id = 2, name = "Clothing", type = TransactionType.EXPENSE, budget = null),
            )
        )
        repository.setCategoryMonthlyTotals(
            listOf(
                CategoryMonthSpend(
                    categoryId = 2, categoryName = "Clothing",
                    year = LocalDate.now().year, month = LocalDate.now().monthValue, total = 310.0,
                )
            )
        )

        val viewModel = OverviewViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            val state = awaitItem() as OverviewUiState.Success
            val clothing = state.budgetSummary.unbudgeted.single()
            assertEquals("Clothing", clothing.label)
            assertEquals(4000.0, clothing.target, 0.0)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
