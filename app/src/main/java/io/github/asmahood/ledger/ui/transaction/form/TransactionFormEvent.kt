package io.github.asmahood.ledger.ui.transaction.form

sealed interface TransactionFormEvent {
    data object SavedSuccessfully : TransactionFormEvent

    /** The form has nothing to show (e.g. the transaction being edited no longer exists); dismiss it. */
    data object Dismissed : TransactionFormEvent

    data class ShowError(val message: String) : TransactionFormEvent
}