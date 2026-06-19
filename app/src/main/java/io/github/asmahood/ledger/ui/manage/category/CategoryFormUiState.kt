package io.github.asmahood.ledger.ui.manage.category

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType

data class CategoryFormUiState(
    val name: String = "",
    val description: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
) {
    val isFormValid: Boolean get() = name.isNotBlank()
}

fun CategoryFormUiState.toCategory(): Category {
    return Category(
        id = 0,
        name = this.name,
        type = this.type,
        description = this.description.ifBlank { null }
    )
}
