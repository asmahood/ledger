package io.github.asmahood.ledger.ui.manage.category

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType

data class CategoryFormUiState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val budget: String = "",
) {
    val isFormValid: Boolean
        get() {
            if (name.isBlank()) return false
            if (budget.isBlank()) return true
            val budgetAmount = budget.toDoubleOrNull()
            return budgetAmount != null && budgetAmount.isFinite() && budgetAmount >= 0.0
        }
}

fun CategoryFormUiState.toCategory(): Category {
    return Category(
        id = this.id,
        name = this.name,
        type = this.type,
        description = this.description.ifBlank { null },
        budget = budgetAmount()
    )
}

fun CategoryFormUiState.budgetAmount(): Double? = budget.toDoubleOrNull()
