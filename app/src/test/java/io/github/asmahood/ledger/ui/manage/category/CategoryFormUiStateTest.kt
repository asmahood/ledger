package io.github.asmahood.ledger.ui.manage.category

import io.github.asmahood.ledger.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryFormUiStateTest {
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
