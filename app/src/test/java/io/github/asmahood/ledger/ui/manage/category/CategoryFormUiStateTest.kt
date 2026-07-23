package io.github.asmahood.ledger.ui.manage.category

import io.github.asmahood.ledger.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryFormUiStateTest {
    // A stray trailing space is invisible in the form but makes the category a different string
    // everywhere it is matched by name — CSV import in particular could never match it.
    @Test
    fun toCategory_trimsSurroundingWhitespaceFromName() {
        val state = CategoryFormUiState(name = "Restaurant ")
        assertEquals("Restaurant", state.toCategory().name)
    }

    @Test
    fun toCategory_trimsSurroundingWhitespaceFromDescription() {
        val state = CategoryFormUiState(name = "Groceries", description = "  weekly shop  ")
        assertEquals("weekly shop", state.toCategory().description)
    }

    @Test
    fun isFormValid_whitespaceOnlyName_isNotValid() {
        assertFalse(CategoryFormUiState(name = "   ").isFormValid)
    }

    @Test
    fun toCategory_blankDescription_mapsToNull() {
        val state = CategoryFormUiState(name = "Groceries", description = "")
        assertNull(state.toCategory().description)
    }

    @Test
    fun toCategory_whitespaceDescription_mapsToNull() {
        val state = CategoryFormUiState(name = "Groceries", description = "   ")
        assertNull(state.toCategory().description)
    }

    @Test
    fun toCategory_populatedFields_mapWithIdZero() {
        val state = CategoryFormUiState(
            name = "Salary",
            description = "Monthly pay",
            type = TransactionType.INCOME,
        )

        val category = state.toCategory()

        assertEquals(0L, category.id)
        assertEquals("Salary", category.name)
        assertEquals(TransactionType.INCOME, category.type)
        assertEquals("Monthly pay", category.description)
    }

    @Test
    fun toCategory_withNonZeroId_preservesId() {
        val state = CategoryFormUiState(id = 42L, name = "Salary", type = TransactionType.INCOME)

        assertEquals(42L, state.toCategory().id)
    }
}
