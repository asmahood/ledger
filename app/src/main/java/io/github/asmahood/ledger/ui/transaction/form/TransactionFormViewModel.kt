package io.github.asmahood.ledger.ui.transaction.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.CategoryRepository
import io.github.asmahood.ledger.data.repository.TransactionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<TransactionFormEvent>()
    val events = _events.receiveAsFlow()

    private var isSaving = false
    private var isLoading = true

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategoriesStream().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
                isLoading = false
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
                repository.insertTransaction(_uiState.value.toTransaction())
                _events.send(TransactionFormEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.send(TransactionFormEvent.ShowError(e.message ?: "Could not save transaction"))
            } finally {
                isSaving = false
            }
        }
    }
}