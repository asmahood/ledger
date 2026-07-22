package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.projection.CategoryMonthSpend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Budget targets are stored per month, so every number the card shows is derived: the target is
 * scaled by the months the period spans, and the Unbudgeted denominator is whatever the income
 * budget leaves over after the expense budgets are honoured.
 */
class OverviewBudgetSummaryTest {

    private val juneStart = LocalDate.of(2026, 6, 1)
    private val juneEnd = LocalDate.of(2026, 6, 30)
    private val aprilStart = LocalDate.of(2026, 4, 1)

    private fun expense(id: Long, name: String, budget: Double? = null) =
        Category(id = id, name = name, type = TransactionType.EXPENSE, budget = budget)

    private fun income(id: Long, name: String, budget: Double? = null) =
        Category(id = id, name = name, type = TransactionType.INCOME, budget = budget)

    private fun spend(id: Long, name: String, month: Int, total: Double) =
        CategoryMonthSpend(
            categoryId = id, categoryName = name, year = 2026, month = month, total = total,
        )

    private fun row(summary: BudgetSummary, label: String): BudgetRow =
        (summary.budgeted + summary.unbudgeted).first { it.label == label }

    // ---- Scaling ----

    @Test
    fun singleMonthPeriod_usesMonthlyBudgetAsIs() {
        val summary = buildBudgetSummary(
            categories = listOf(expense(1, "Groceries", budget = 400.0)),
            spend = listOf(spend(1, "Groceries", month = 6, total = 310.0)),
            start = juneStart, end = juneEnd,
        )

        val groceries = row(summary, "Groceries")
        assertEquals(400.0, groceries.target, 0.0)
        assertEquals(310.0, groceries.actual, 0.0)
        assertEquals(78, groceries.percent)
    }

    @Test
    fun threeMonthPeriod_multipliesBudgetByThree() {
        val summary = buildBudgetSummary(
            categories = listOf(expense(1, "Groceries", budget = 400.0)),
            spend = listOf(spend(1, "Groceries", month = 6, total = 310.0)),
            start = aprilStart, end = juneEnd,
        )

        assertEquals(1200.0, row(summary, "Groceries").target, 0.0)
    }

    @Test
    fun actualSpend_sumsEveryMonthInThePeriod() {
        val summary = buildBudgetSummary(
            categories = listOf(expense(1, "Groceries", budget = 400.0)),
            spend = listOf(
                spend(1, "Groceries", month = 4, total = 100.0),
                spend(1, "Groceries", month = 5, total = 250.0),
                spend(1, "Groceries", month = 6, total = 310.0),
            ),
            start = aprilStart, end = juneEnd,
        )

        assertEquals(660.0, row(summary, "Groceries").actual, 0.0)
    }

    // ---- Membership ----

    @Test
    fun budgetedCategoryWithNoSpend_stillAppears() {
        val summary = buildBudgetSummary(
            categories = listOf(expense(1, "Mobile", budget = 75.0)),
            spend = emptyList(),
            start = juneStart, end = juneEnd,
        )

        val mobile = summary.budgeted.single()
        assertEquals("Mobile", mobile.label)
        assertEquals(0.0, mobile.actual, 0.0)
        assertEquals(75.0, mobile.target, 0.0)
        assertEquals(0, mobile.percent)
    }

    // A category with neither a budget nor any spend is noise — it has nothing to report.
    @Test
    fun unbudgetedCategoryWithNoSpend_isHidden() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 4000.0),
                expense(1, "Clothing"),
                expense(2, "Electronics"),
            ),
            spend = listOf(spend(2, "Electronics", month = 6, total = 10.0)),
            start = juneStart, end = juneEnd,
        )

        assertEquals(listOf("Electronics"), summary.unbudgeted.map { it.label })
    }

    @Test
    fun incomeCategories_areNeverListedAsRows() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 4000.0),
                income(8, "Interest"),
                expense(1, "Groceries", budget = 400.0),
            ),
            spend = listOf(spend(9, "Salary", month = 6, total = 4200.0)),
            start = juneStart, end = juneEnd,
        )

        val labels = (summary.budgeted + summary.unbudgeted).map { it.label }
        assertTrue("Income categories leaked into the rows: $labels", labels.none { it == "Salary" })
        assertTrue("Income categories leaked into the rows: $labels", labels.none { it == "Interest" })
    }

    @Test
    fun rows_areSortedByCategoryName() {
        val summary = buildBudgetSummary(
            categories = listOf(
                expense(1, "Restaurant", budget = 200.0),
                expense(2, "Groceries", budget = 400.0),
                expense(3, "Mobile", budget = 75.0),
            ),
            spend = emptyList(),
            start = juneStart, end = juneEnd,
        )

        assertEquals(listOf("Groceries", "Mobile", "Restaurant"), summary.budgeted.map { it.label })
    }

    @Test
    fun noBudgetsAnywhere_putsEveryExpenseCategoryInUnbudgeted() {
        val summary = buildBudgetSummary(
            categories = listOf(expense(1, "Groceries"), expense(2, "Clothing")),
            spend = listOf(
                spend(1, "Groceries", month = 6, total = 310.0),
                spend(2, "Clothing", month = 6, total = 90.0),
            ),
            start = juneStart, end = juneEnd,
        )

        assertTrue(summary.budgeted.isEmpty())
        assertEquals(listOf("Clothing", "Groceries"), summary.unbudgeted.map { it.label })
    }

    @Test
    fun noCategoriesAtAll_producesEmptySectionsAndZeroTotals() {
        val summary = buildBudgetSummary(
            categories = emptyList(), spend = emptyList(),
            start = juneStart, end = juneEnd,
        )

        assertTrue(summary.budgeted.isEmpty())
        assertTrue(summary.unbudgeted.isEmpty())
        assertEquals(0.0, summary.budgetedTotal.actual, 0.0)
        assertEquals(0.0, summary.budgetedTotal.target, 0.0)
        assertEquals(0.0, summary.unbudgetedTotal.target, 0.0)
    }

    // ---- Implied savings ----

    @Test
    fun impliedSavings_isBudgetedIncomeMinusBudgetedExpenses() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 4000.0),
                expense(1, "Groceries", budget = 400.0),
                expense(2, "Mobile", budget = 75.0),
                expense(3, "Clothing"),
            ),
            spend = listOf(spend(3, "Clothing", month = 6, total = 310.0)),
            start = juneStart, end = juneEnd,
        )

        assertEquals(3525.0, row(summary, "Clothing").target, 0.0)
    }

    @Test
    fun impliedSavings_scalesWithMonthCount() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 4000.0),
                expense(1, "Groceries", budget = 400.0),
                expense(3, "Clothing"),
            ),
            spend = listOf(spend(3, "Clothing", month = 6, total = 310.0)),
            start = aprilStart, end = juneEnd,
        )

        // (4000 − 400) × 3
        assertEquals(10800.0, row(summary, "Clothing").target, 0.0)
    }

    @Test
    fun multipleIncomeBudgets_areSummed() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 4000.0),
                income(8, "Freelance", budget = 500.0),
                expense(3, "Clothing"),
            ),
            spend = listOf(spend(3, "Clothing", month = 6, total = 310.0)),
            start = juneStart, end = juneEnd,
        )

        assertEquals(4500.0, row(summary, "Clothing").target, 0.0)
    }

    @Test
    fun everyUnbudgetedRow_drawsFromTheFullImpliedSavingsPool() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 4000.0),
                expense(1, "Clothing"),
                expense(2, "Electronics"),
            ),
            spend = listOf(
                spend(1, "Clothing", month = 6, total = 310.0),
                spend(2, "Electronics", month = 6, total = 10.0),
            ),
            start = juneStart, end = juneEnd,
        )

        assertEquals(4000.0, row(summary, "Clothing").target, 0.0)
        assertEquals(4000.0, row(summary, "Electronics").target, 0.0)
    }

    @Test
    fun expenseBudgetsExceedingIncomeBudget_clampImpliedSavingsToZero() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 1000.0),
                expense(1, "Rent", budget = 1500.0),
                expense(3, "Clothing"),
            ),
            spend = listOf(spend(3, "Clothing", month = 6, total = 310.0)),
            start = juneStart, end = juneEnd,
        )

        val clothing = row(summary, "Clothing")
        assertEquals(0.0, clothing.target, 0.0)
        assertNull(clothing.percent)
        assertEquals(BudgetStatus.OVER, clothing.status)
    }

    @Test
    fun noIncomeBudgetSet_leavesImpliedSavingsAtZero() {
        val summary = buildBudgetSummary(
            categories = listOf(income(9, "Salary"), expense(3, "Clothing")),
            spend = listOf(spend(3, "Clothing", month = 6, total = 310.0)),
            start = juneStart, end = juneEnd,
        )

        assertEquals(0.0, summary.unbudgetedTotal.target, 0.0)
        assertEquals(BudgetStatus.OVER, row(summary, "Clothing").status)
    }

    // ---- Totals ----

    @Test
    fun budgetedTotal_sumsActualsAndScaledTargets() {
        val summary = buildBudgetSummary(
            categories = listOf(
                expense(1, "Groceries", budget = 400.0),
                expense(2, "Restaurant", budget = 200.0),
                expense(3, "Mobile", budget = 75.0),
                expense(4, "Going Out", budget = 250.0),
            ),
            spend = listOf(
                spend(1, "Groceries", month = 6, total = 310.0),
                spend(2, "Restaurant", month = 6, total = 340.0),
                spend(4, "Going Out", month = 6, total = 50.0),
            ),
            start = juneStart, end = juneEnd,
        )

        assertEquals(700.0, summary.budgetedTotal.actual, 0.0)
        assertEquals(925.0, summary.budgetedTotal.target, 0.0)
        assertEquals(76, summary.budgetedTotal.percent)
        assertEquals(BudgetStatus.UNDER, summary.budgetedTotal.status)
    }

    @Test
    fun unbudgetedTotal_usesImpliedSavingsAsItsTarget() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 4000.0),
                expense(1, "Groceries", budget = 400.0),
                expense(2, "Clothing"),
                expense(3, "Electronics"),
            ),
            spend = listOf(
                spend(2, "Clothing", month = 6, total = 310.0),
                spend(3, "Electronics", month = 6, total = 10.0),
            ),
            start = juneStart, end = juneEnd,
        )

        assertEquals(320.0, summary.unbudgetedTotal.actual, 0.0)
        assertEquals(3600.0, summary.unbudgetedTotal.target, 0.0)
        assertEquals(9, summary.unbudgetedTotal.percent)
    }

    @Test
    fun unbudgetedTotalBeyondImpliedSavings_isOver() {
        val summary = buildBudgetSummary(
            categories = listOf(
                income(9, "Salary", budget = 1000.0),
                expense(1, "Groceries", budget = 400.0),
                expense(2, "Clothing"),
            ),
            spend = listOf(spend(2, "Clothing", month = 6, total = 900.0)),
            start = juneStart, end = juneEnd,
        )

        assertEquals(BudgetStatus.OVER, summary.unbudgetedTotal.status)
    }

    @Test
    fun budgetedTotalOverBudget_isOver() {
        val summary = buildBudgetSummary(
            categories = listOf(expense(1, "Restaurant", budget = 200.0)),
            spend = listOf(spend(1, "Restaurant", month = 6, total = 340.0)),
            start = juneStart, end = juneEnd,
        )

        assertEquals(BudgetStatus.OVER, summary.budgetedTotal.status)
        assertEquals(170, summary.budgetedTotal.percent)
    }

    // Spend from a deleted category can linger in the projection; it must not crash or invent rows.
    @Test
    fun spendForAnUnknownCategory_isIgnored() {
        val summary = buildBudgetSummary(
            categories = listOf(expense(1, "Groceries", budget = 400.0)),
            spend = listOf(
                spend(1, "Groceries", month = 6, total = 310.0),
                spend(99, "Deleted", month = 6, total = 50.0),
            ),
            start = juneStart, end = juneEnd,
        )

        assertEquals(listOf("Groceries"), summary.budgeted.map { it.label })
        assertTrue(summary.unbudgeted.isEmpty())
        assertEquals(310.0, summary.budgetedTotal.actual, 0.0)
    }
}
