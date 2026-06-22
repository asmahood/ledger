package io.github.asmahood.ledger.ui.transaction.form

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionFormUiStateTest {
    private val expenseCategory =
        Category(id = 1L, name = "Groceries", type = TransactionType.EXPENSE)
    private val incomeCategory =
        Category(id = 2L, name = "Salary", type = TransactionType.INCOME)

    // A fully-populated, valid expense state used as the baseline for validity tests.
    private fun validState() = TransactionFormUiState(
        amount = "12.50",
        date = "06/21/2026",
        vendor = "Food Basics",
        type = TransactionType.EXPENSE,
        category = expenseCategory,
    )

    // ── isFormValid ───────────────────────────────────────────────────────────────

    @Test
    fun isFormValid_allFieldsPopulated_isTrue() {
        assertTrue(validState().isFormValid)
    }

    @Test
    fun isFormValid_blankAmount_isFalse() {
        assertFalse(validState().copy(amount = "").isFormValid)
    }

    @Test
    fun isFormValid_nonNumericAmount_isFalse() {
        assertFalse(validState().copy(amount = "abc").isFormValid)
    }

    @Test
    fun isFormValid_zeroAmount_isFalse() {
        assertFalse(validState().copy(amount = "0").isFormValid)
    }

    @Test
    fun isFormValid_negativeAmount_isFalse() {
        assertFalse(validState().copy(amount = "-5").isFormValid)
    }

    @Test
    fun isFormValid_blankVendor_isFalse() {
        assertFalse(validState().copy(vendor = "   ").isFormValid)
    }

    @Test
    fun isFormValid_blankDate_isFalse() {
        assertFalse(validState().copy(date = "").isFormValid)
    }

    @Test
    fun isFormValid_nullCategory_isFalse() {
        assertFalse(validState().copy(category = null).isFormValid)
    }

    @Test
    fun isFormValid_notesOptional_remainsValidWhenBlank() {
        assertTrue(validState().copy(notes = "").isFormValid)
    }

    // ── visibleCategories ─────────────────────────────────────────────────────────

    @Test
    fun visibleCategories_filtersByCurrentType() {
        val state = TransactionFormUiState(
            type = TransactionType.EXPENSE,
            categories = listOf(expenseCategory, incomeCategory),
        )

        assertEquals(listOf(expenseCategory), state.visibleCategories)
    }

    @Test
    fun visibleCategories_income_showsOnlyIncomeCategories() {
        val state = TransactionFormUiState(
            type = TransactionType.INCOME,
            categories = listOf(expenseCategory, incomeCategory),
        )

        assertEquals(listOf(incomeCategory), state.visibleCategories)
    }

    // ── toTransaction ─────────────────────────────────────────────────────────────

    @Test
    fun toTransaction_mapsAllFields() {
        val transaction = validState().copy(id = 7L, notes = "lunch").toTransaction()

        assertEquals(7L, transaction.id)
        assertEquals(12.50, transaction.amount, 0.0)
        assertEquals(LocalDate.of(2026, 6, 21), transaction.date)
        assertEquals("Food Basics", transaction.vendor)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals("lunch", transaction.notes)
        assertEquals(expenseCategory, transaction.category)
    }

    @Test
    fun toTransaction_defaultId_isZero() {
        assertEquals(0L, validState().toTransaction().id)
    }
}
