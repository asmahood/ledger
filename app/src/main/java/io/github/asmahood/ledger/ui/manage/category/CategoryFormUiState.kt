package io.github.asmahood.ledger.ui.manage.category

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.MonthlyAmountStats
import io.github.asmahood.ledger.data.model.TransactionType

data class CategoryFormUiState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val budget: String = "",
    val monthlyStats: MonthlyAmountStats? = null,
    val isStatsLoaded: Boolean = false
) {
    val isFormValid: Boolean
        get() {
            if (name.isBlank()) return false
            if (budget.isBlank()) return true
            val budgetAmount = budget.toDoubleOrNull()
            return budgetAmount != null && budgetAmount.isFinite() && budgetAmount >= 0.0
        }
}

/**
 * Trims on the way out rather than as the user types, so a space typed mid-word isn't swallowed
 * before the next character arrives. Names are matched by value elsewhere — CSV import, the
 * unique-name constraint — and a stray edge space is invisible in the form but makes the category
 * a different string to every one of those comparisons.
 */
fun CategoryFormUiState.toCategory(): Category {
    return Category(
        id = this.id,
        name = this.name.trim(),
        type = this.type,
        description = this.description.trim().ifBlank { null },
    )
}

fun CategoryFormUiState.budgetAmount(): Double? = budget.toDoubleOrNull()
