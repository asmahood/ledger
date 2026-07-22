package io.github.asmahood.ledger.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The overview income/expense/savings bars aren't assertable through Compose semantics (they paint
 * onto a canvas), so their meaning is locked in here: income reads green, expense reads red, savings
 * reads blue, and the three are always distinguishable — in both the light and dark scheme. The
 * budget bar's warning band is pinned the same way, for the same reason.
 */
class ChartColorsTest {

    private fun assertGreenDominant(color: Color, name: String) {
        assertTrue("$name should be greener than it is red", color.green > color.red)
        assertTrue("$name should be greener than it is blue", color.green > color.blue)
    }

    private fun assertRedDominant(color: Color, name: String) {
        assertTrue("$name should be redder than it is green", color.red > color.green)
        assertTrue("$name should be redder than it is blue", color.red > color.blue)
    }

    private fun assertBlueDominant(color: Color, name: String) {
        assertTrue("$name should be bluer than it is red", color.blue > color.red)
        assertTrue("$name should be bluer than it is green", color.blue > color.green)
    }

    // Amber carries both red and green; what makes it read as a warning rather than as income is
    // the near-absent blue channel plus a red lean.
    private fun assertAmber(color: Color, name: String) {
        assertTrue("$name should carry more red than blue", color.red > color.blue)
        assertTrue("$name should carry more green than blue", color.green > color.blue)
        assertTrue("$name should lean red rather than green", color.red > color.green)
    }

    @Test
    fun incomeColors_areGreenDominant() {
        assertGreenDominant(incomeLight, "incomeLight")
        assertGreenDominant(incomeDark, "incomeDark")
    }

    @Test
    fun expenseColors_areRedDominant() {
        assertRedDominant(expenseLight, "expenseLight")
        assertRedDominant(expenseDark, "expenseDark")
    }

    @Test
    fun savingsColors_areBlueDominant() {
        assertBlueDominant(savingsLight, "savingsLight")
        assertBlueDominant(savingsDark, "savingsDark")
    }

    @Test
    fun warningColors_areAmber() {
        assertAmber(warningLight, "warningLight")
        assertAmber(warningDark, "warningDark")
    }

    @Test
    fun warning_isDistinctFromIncomeAndExpense() {
        assertNotEquals(warningLight, incomeLight)
        assertNotEquals(warningDark, incomeDark)
        assertNotEquals(warningLight, expenseLight)
        assertNotEquals(warningDark, expenseDark)
    }

    @Test
    fun incomeExpenseAndSavings_areDistinctInBothSchemes() {
        assertNotEquals(incomeLight, expenseLight)
        assertNotEquals(incomeDark, expenseDark)
        assertNotEquals(incomeLight, savingsLight)
        assertNotEquals(incomeDark, savingsDark)
        assertNotEquals(expenseLight, savingsLight)
        assertNotEquals(expenseDark, savingsDark)
    }
}
