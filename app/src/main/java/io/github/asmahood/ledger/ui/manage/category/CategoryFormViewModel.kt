package io.github.asmahood.ledger.ui.manage.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.CategoryRepository
import io.github.asmahood.ledger.data.repository.DuplicateCategoryException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryFormViewModel @Inject constructor(private val repository: CategoryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<CategoryFormEvent>()
    val events = _events.receiveAsFlow()

    private var isSaving = false

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun updateType(value: TransactionType) {
        _uiState.update { it.copy(type = value) }
    }

    fun saveCategory() {
        if (!_uiState.value.isFormValid || isSaving) return
        isSaving = true

        viewModelScope.launch {
            try {
                repository.insertCategory(_uiState.value.toCategory())
                _events.send(CategoryFormEvent.SavedSuccessfully)
            } catch (e: DuplicateCategoryException) {
                _events.send(CategoryFormEvent.ShowError(e.message ?: "Could not save category"))
            } finally {
                isSaving = false
            }
        }
    }
}