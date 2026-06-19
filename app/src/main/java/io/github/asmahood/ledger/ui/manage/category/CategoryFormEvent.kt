package io.github.asmahood.ledger.ui.manage.category

sealed interface CategoryFormEvent {
    data object SavedSuccessfully : CategoryFormEvent
    data class ShowError(val message: String) : CategoryFormEvent
}