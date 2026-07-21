package io.github.asmahood.ledger.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The overview income/expense bars aren't assertable through Compose semantics (they paint onto a
 * canvas), so their meaning is locked in here: income reads green, expense reads red, and the two
 * are always distinguishable — in both the light and dark scheme.
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
    fun incomeAndExpense_areDistinctInBothSchemes() {
        assertNotEquals(incomeLight, expenseLight)
        assertNotEquals(incomeDark, expenseDark)
    }
}
