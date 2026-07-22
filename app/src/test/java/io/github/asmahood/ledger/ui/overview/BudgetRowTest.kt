package io.github.asmahood.ledger.ui.overview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [BudgetRow.of] is where a pair of dollar amounts becomes a percentage, a status and a bar
 * fraction — the three things the card renders. Every band boundary and every divide-by-zero path
 * is pinned here so the composable can stay decision-free.
 */
class BudgetRowTest {

    private fun budgeted(actual: Double, target: Double) =
        BudgetRow.of("Groceries", actual, target, unbudgeted = false)

    private fun unbudgeted(actual: Double, target: Double) =
        BudgetRow.of("Clothing", actual, target, unbudgeted = true)

    @Test
    fun percent_roundsToNearestWholeNumber() {
        assertEquals(78, budgeted(310.0, 400.0).percent)
        assertEquals(170, budgeted(340.0, 200.0).percent)
        assertEquals(97, budgeted(700.0, 725.0).percent)
    }

    @Test
    fun spendWellUnderBudget_isUnder() {
        assertEquals(BudgetStatus.UNDER, budgeted(50.0, 250.0).status)
    }

    @Test
    fun spendAtEightyPercent_isStillUnder() {
        assertEquals(BudgetStatus.UNDER, budgeted(320.0, 400.0).status)
    }

    @Test
    fun spendJustAboveEightyPercent_isNear() {
        assertEquals(BudgetStatus.NEAR, budgeted(324.0, 400.0).status)
    }

    @Test
    fun spendExactlyAtBudget_isNear() {
        assertEquals(BudgetStatus.NEAR, budgeted(400.0, 400.0).status)
    }

    @Test
    fun spendOverBudget_isOver() {
        assertEquals(BudgetStatus.OVER, budgeted(340.0, 200.0).status)
    }

    @Test
    fun zeroSpend_isUnderWithZeroPercent() {
        val row = budgeted(0.0, 75.0)
        assertEquals(0, row.percent)
        assertEquals(BudgetStatus.UNDER, row.status)
        assertEquals(0f, row.fraction, 0f)
    }

    // Unbudgeted rows are flat blue regardless of how full they are; only overflow changes colour.
    @Test
    fun unbudgetedRowUnderTarget_keepsUnbudgetedStatus() {
        assertEquals(BudgetStatus.UNBUDGETED, unbudgeted(310.0, 400.0).status)
        assertEquals(BudgetStatus.UNBUDGETED, unbudgeted(400.0, 400.0).status)
    }

    @Test
    fun unbudgetedRowOverTarget_isOver() {
        assertEquals(BudgetStatus.OVER, unbudgeted(500.0, 400.0).status)
    }

    // Implied savings clamps to zero when expense budgets swallow the income budget. Any spend at
    // all is then over budget, and there is no meaningful percentage to show.
    @Test
    fun zeroTargetWithSpend_isOverWithNoPercent() {
        val row = unbudgeted(310.0, 0.0)
        assertNull(row.percent)
        assertEquals(BudgetStatus.OVER, row.status)
        assertEquals(1f, row.fraction, 0f)
    }

    @Test
    fun zeroTargetWithNoSpend_isNotOver() {
        val row = unbudgeted(0.0, 0.0)
        assertNull(row.percent)
        assertEquals(BudgetStatus.UNBUDGETED, row.status)
        assertEquals(0f, row.fraction, 0f)
    }

    @Test
    fun fraction_clampsToOneWhenOverBudget() {
        assertEquals(1f, budgeted(340.0, 200.0).fraction, 0f)
    }

    @Test
    fun fraction_isProportionalBelowBudget() {
        assertEquals(0.775f, budgeted(310.0, 400.0).fraction, 0.0001f)
    }

    @Test
    fun negativeTarget_isTreatedAsZero() {
        val row = unbudgeted(100.0, -50.0)
        assertNull(row.percent)
        assertEquals(BudgetStatus.OVER, row.status)
    }
}
