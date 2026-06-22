package io.github.asmahood.ledger.ui.transaction.form

sealed interface TransactionFormEvent {
    data object SavedSuccessfully : TransactionFormEvent
    data class ShowError(val message: String) : TransactionFormEvent
}