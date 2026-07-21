package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.MonthlyTotal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OverviewTotalSavingsChartTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val end = LocalDate.of(2026, 3, 31)

    private fun total(year: Int = 2026, month: Int, total: Double) =
        MonthlyTotal(year = year, month = month, total = total)

    /** Builds the savings chart the way the view model does: from the two aligned monthly charts. */
    private fun savings(
        income: List<MonthlyTotal>,
        expense: List<MonthlyTotal>,
        rangeStart: LocalDate = start,
        rangeEnd: LocalDate = end,
    ) = income.toTotalIncomeChart(rangeStart, rangeEnd)
        .toTotalSavingsChart(expense.toTotalExpenseChart(rangeStart, rangeEnd))

    @Test
    fun toTotalSavingsChart_carriesTheSharedMonthLabels() {
        val chart = savings(emptyList(), emptyList())

        assertEquals(listOf("Jan '26", "Feb '26", "Mar '26"), chart.monthLabels)
    }

    @Test
    fun toTotalSavingsChart_emptyInputs_amountsAreZeroAndParallelToLabels() {
        val chart = savings(emptyList(), emptyList())

        assertEquals(3, chart.monthLabels.size)
        assertEquals(chart.monthLabels.size, chart.amounts.size)
        assertEquals(listOf(0.0, 0.0, 0.0), chart.amounts)
    }

    @Test
    fun toTotalSavingsChart_subtractsExpenseFromIncomePerMonth() {
        val income = listOf(
            total(month = 1, total = 4000.0),
            total(month = 2, total = 3950.0),
            total(month = 3, total = 4600.0),
        )
        val expense = listOf(
            total(month = 1, total = 3000.0),
            total(month = 2, total = 2950.0),
            total(month = 3, total = 3600.0),
        )

        val chart = savings(income, expense)

        assertEquals(listOf(1000.0, 1000.0, 1000.0), chart.amounts)
    }

    @Test
    fun toTotalSavingsChart_expenseExceedsIncome_producesNegativeSavings() {
        // A month that overspends nets below zero; the chart must carry the sign, not clamp it.
        val income = listOf(total(month = 2, total = 3100.0))
        val expense = listOf(total(month = 2, total = 4200.0))

        val chart = savings(income, expense)

        assertEquals(listOf(0.0, -1100.0, 0.0), chart.amounts)
    }

    @Test
    fun toTotalSavingsChart_incomeMonthWithNoExpense_countsFullIncomeAsSaved() {
        // Expense stream is missing February; the zero-filled expense chart still nets correctly.
        val income = listOf(total(month = 2, total = 3950.0))
        val expense = emptyList<MonthlyTotal>()

        val chart = savings(income, expense)

        assertEquals(listOf(0.0, 3950.0, 0.0), chart.amounts)
    }

    @Test
    fun toTotalSavingsChart_expenseMonthWithNoIncome_countsFullExpenseAsNegative() {
        // The mirror of the above: an expense with no matching income nets fully negative.
        val income = emptyList<MonthlyTotal>()
        val expense = listOf(total(month = 3, total = 3600.0))

        val chart = savings(income, expense)

        assertEquals(listOf(0.0, 0.0, -3600.0), chart.amounts)
    }

    @Test
    fun toTotalSavingsChart_amountsAreAlwaysParallelToMonthLabels() {
        val chart = savings(
            listOf(total(month = 2, total = 3950.0)),
            listOf(total(month = 2, total = 2950.0)),
        )

        assertEquals(chart.monthLabels.size, chart.amounts.size)
    }

    @Test
    fun toTotalSavingsChart_spanningYearBoundary_alignsNetToMonthOrder() {
        val income = listOf(
            total(year = 2025, month = 12, total = 3800.0),
            total(year = 2026, month = 1, total = 4000.0),
        )
        val expense = listOf(
            total(year = 2025, month = 12, total = 2800.0),
            total(year = 2026, month = 1, total = 3000.0),
        )

        val chart = savings(
            income,
            expense,
            rangeStart = LocalDate.of(2025, 12, 1),
            rangeEnd = LocalDate.of(2026, 1, 31),
        )

        assertEquals(listOf("Dec '25", "Jan '26"), chart.monthLabels)
        assertEquals(listOf(1000.0, 1000.0), chart.amounts)
    }
}
