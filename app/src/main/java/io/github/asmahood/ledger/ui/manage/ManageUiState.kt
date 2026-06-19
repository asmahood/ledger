package io.github.asmahood.ledger.ui.manage

import io.github.asmahood.ledger.data.model.Category

sealed interface ManageUiState {
    data object Loading : ManageUiState
    data class Success(
        val expenseCategories: List<Category> = emptyList(),
        val incomeCategories: List<Category> = emptyList(),
    ) : ManageUiState
    data class Error(val message: String) : ManageUiState
}