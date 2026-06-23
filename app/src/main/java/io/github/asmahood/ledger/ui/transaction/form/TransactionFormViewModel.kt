package io.github.asmahood.ledger.ui.transaction.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.CategoryRepository
import io.github.asmahood.ledger.data.repository.TransactionRepository
import io.github.asmahood.ledger.util.transactionDateFormatter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<TransactionFormEvent>()
    val events = _events.receiveAsFlow()

    private val transactionId: Long? = savedStateHandle["transactionId"]

    private var isSaving = false
    private var categoriesLoaded = false
    // In add mode there is no transaction to load, so it is "loaded" from the start.
    private var transactionLoaded = transactionId == null

    // The form is loading until both the categories and (in edit mode) the transaction have arrived.
    private val isLoading: Boolean
        get() = !(categoriesLoaded && transactionLoaded)

    init {
        if (transactionId != null) {
            _uiState.update { it.copy(isEditMode = true) }
        }

        viewModelScope.launch {
            categoryRepository.getAllCategoriesStream().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
                categoriesLoaded = true
            }
        }

        if (transactionId != null) {
            viewModelScope.launch {
                try {
                    val transaction = repository.getTransactionStream(transactionId).first()
                    if (transaction != null) {
                        _uiState.update {
                            it.copy(
                                id = transaction.id,
                                type = transaction.type,
                                amount = String.format(Locale.US, "%.2f", transaction.amount),
                                category = transaction.category,
                                date = transaction.date.format(transactionDateFormatter),
                                vendor = transaction.vendor,
                                notes = transaction.notes ?: "",
                            )
                        }
                    } else {
                        _events.send(TransactionFormEvent.Dismissed)
                    }
                } finally {
                    transactionLoaded = true
                }
            }
        }
    }

    fun updateType(value: TransactionType) {
        _uiState.update {
            val keep = it.category?.takeIf { c -> c.type == value }
            it.copy(type = value, category = keep)
        }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amount = value) }
    }

    fun updateCategory(value: Category) {
        _uiState.update { it.copy(category = value) }
    }

    fun updateDate(value: String) {
        _uiState.update { it.copy(date = value) }
    }

    fun updateVendor(value: String) {
        _uiState.update { it.copy(vendor = value) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun saveTransaction() {
        if (!_uiState.value.isFormValid || isSaving || isLoading) return
        isSaving = true

        viewModelScope.launch {
            try {
                if (transactionId == null) {
                    repository.insertTransaction(_uiState.value.toTransaction())
                } else {
                    repository.updateTransaction(_uiState.value.toTransaction())
                }
                _events.send(TransactionFormEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.send(
                    TransactionFormEvent.ShowError(
                        e.message ?: "Could not save transaction"
                    )
                )
            } finally {
                isSaving = false
            }
        }
    }

    fun deleteTransaction() {
        if (isSaving || isLoading) return
        isSaving = true
        viewModelScope.launch {
            try {
                repository.deleteTransaction(_uiState.value.toTransaction())
                _events.send(TransactionFormEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.send(TransactionFormEvent.ShowError(e.message ?: "Could not delete transaction"))
            } finally {
                isSaving = false
            }
        }
    }
}