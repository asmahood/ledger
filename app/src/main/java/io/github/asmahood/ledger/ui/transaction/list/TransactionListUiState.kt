package io.github.asmahood.ledger.ui.transaction.list

import io.github.asmahood.ledger.data.model.TransactionDayGroup

sealed interface TransactionListUiState {
    data object Loading : TransactionListUiState
    data class Success(val groups: List<TransactionDayGroup>) : TransactionListUiState
    data class Error(val message: String) : TransactionListUiState
}
