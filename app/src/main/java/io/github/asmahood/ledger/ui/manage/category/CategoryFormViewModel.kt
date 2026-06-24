package io.github.asmahood.ledger.ui.manage.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.CategoryRepository
import io.github.asmahood.ledger.data.repository.TransactionRepository
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
class CategoryFormViewModel @Inject constructor(
    private val repository: CategoryRepository,
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<CategoryFormEvent>()
    val events = _events.receiveAsFlow()

    private val categoryId: Long? = savedStateHandle["categoryId"]

    private var isSaving = false
    private var isLoading = false

    init {
        if (categoryId != null) {
            loadCategory(categoryId)
            observeMonthlyStats(categoryId)
        }
    }

    private fun loadCategory(categoryId: Long) {
        isLoading = true
        viewModelScope.launch {
            try {
                val category = repository.getCategoryStream(categoryId).first()
                if (category != null) {
                    _uiState.update {
                        it.copy(
                            id = category.id,
                            name = category.name,
                            description = category.description ?: "",
                            type = category.type,
                            budget = category.budget?.let {
                                String.format(Locale.US, "%.2f", it)
                            } ?: ""
                        )
                    }
                } else {
                    _events.send(CategoryFormEvent.SavedSuccessfully)
                }
            } finally {
                isLoading = false
            }
        }
    }

    private fun observeMonthlyStats(categoryId: Long) {
        viewModelScope.launch {
            transactionRepository.getMonthlyAmountStatsStream(categoryId).collect { stats ->
                _uiState.update { it.copy(monthlyStats = stats, isStatsLoaded = true) }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun updateType(value: TransactionType) {
        _uiState.update { it.copy(type = value) }
    }

    fun updateBudget(value: String) {
        _uiState.update { it.copy(budget = value.trim()) }
    }

    fun saveCategory() {
        if (!_uiState.value.isFormValid || isSaving || isLoading) return
        isSaving = true

        viewModelScope.launch {
            try {
                if (categoryId == null) {
                    repository.insertCategoryWithBudget(_uiState.value.toCategory(), _uiState.value.budgetAmount())
                } else {
                    repository.updateCategoryWithBudget(_uiState.value.toCategory(), _uiState.value.budgetAmount())
                }
                _events.send(CategoryFormEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.send(CategoryFormEvent.ShowError(e.message ?: "Could not save category"))
            } finally {
                isSaving = false
            }
        }
    }

    fun deleteCategory() {
        if (isSaving || isLoading) return
        isSaving = true
        viewModelScope.launch {
            try {
                repository.deleteCategory(_uiState.value.toCategory())
                _events.send(CategoryFormEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.send(CategoryFormEvent.ShowError(e.message ?: "Could not delete category"))
            } finally {
                isSaving = false
            }
        }
    }
}