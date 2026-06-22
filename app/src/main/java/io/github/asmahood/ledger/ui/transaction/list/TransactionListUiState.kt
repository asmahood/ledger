package io.github.asmahood.ledger.ui.transaction.list

import io.github.asmahood.ledger.data.model.Transaction

sealed interface TransactionListUiState {
    data object Loading : TransactionListUiState
    data class Success(val transactions: List<Transaction>) : TransactionListUiState
    data class Error(val message: String) : TransactionListUiState
}
