package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.PeriodTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewSummaryTest {
    @Test
    fun toSummary_computesSavedAndRoundedPercentages() {
        val summary = PeriodTotals(income = 4200.0, expenses = 3100.0).toSummary()

        assertEquals(4200.0, summary.income, 0.0)
        assertEquals(3100.0, summary.expenses, 0.0)
        assertEquals(1100.0, summary.saved, 0.0)
        // 3100/4200 = 73.8% -> 74, 1100/4200 = 26.2% -> 26
        assertEquals(74, summary.expensesPercentOfIncome)
        assertEquals(26, summary.savedPercentOfIncome)
        assertFalse(summary.isNegativeSaved)
    }

    @Test
    fun toSummary_nullTotals_treatedAsZero() {
        val summary = PeriodTotals(income = null, expenses = null).toSummary()

        assertEquals(0.0, summary.income, 0.0)
        assertEquals(0.0, summary.expenses, 0.0)
        assertEquals(0.0, summary.saved, 0.0)
        // No income means no meaningful percentage.
        assertNull(summary.expensesPercentOfIncome)
        assertNull(summary.savedPercentOfIncome)
    }

    @Test
    fun toSummary_zeroIncomeWithExpenses_percentagesAreNull() {
        val summary = PeriodTotals(income = 0.0, expenses = 500.0).toSummary()

        assertEquals(-500.0, summary.saved, 0.0)
        assertNull(summary.expensesPercentOfIncome)
        assertNull(summary.savedPercentOfIncome)
        assertTrue(summary.isNegativeSaved)
    }

    @Test
    fun toSummary_expensesExceedIncome_savedIsNegative() {
        val summary = PeriodTotals(income = 1000.0, expenses = 1500.0).toSummary()

        assertEquals(-500.0, summary.saved, 0.0)
        assertTrue(summary.isNegativeSaved)
        assertEquals(150, summary.expensesPercentOfIncome)
        assertEquals(-50, summary.savedPercentOfIncome)
    }

    @Test
    fun toSummary_incomeOnly_savedEqualsIncome() {
        val summary = PeriodTotals(income = 2000.0, expenses = null).toSummary()

        assertEquals(2000.0, summary.saved, 0.0)
        assertEquals(0, summary.expensesPercentOfIncome)
        assertEquals(100, summary.savedPercentOfIncome)
        assertFalse(summary.isNegativeSaved)
    }

    @Test
    fun toSummary_incomeOnlyZeroExpenses_matchesNullExpenses() {
        // The DAO returns expenses = 0.0 (not null) when rows exist but none are expenses.
        val summary = PeriodTotals(income = 2000.0, expenses = 0.0).toSummary()

        assertEquals(2000.0, summary.saved, 0.0)
        assertEquals(0, summary.expensesPercentOfIncome)
        assertEquals(100, summary.savedPercentOfIncome)
        assertFalse(summary.isNegativeSaved)
    }

    @Test
    fun toSummary_extremeRatio_percentClampedToIntRange() {
        val summary = PeriodTotals(income = 1.0, expenses = 1_000_000_000.0).toSummary()

        assertEquals(Int.MAX_VALUE, summary.expensesPercentOfIncome)
    }
}
