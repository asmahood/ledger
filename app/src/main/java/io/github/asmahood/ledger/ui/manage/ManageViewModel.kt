package io.github.asmahood.ledger.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ManageViewModel @Inject constructor(
    private val repository: CategoryRepository,
) : ViewModel() {
    val uiState: StateFlow<ManageUiState> = repository.getAlLCategoriesStream()
        .map<List<Category>, ManageUiState> { categories ->
            val (expenses, income) = categories.partition { it.type == TransactionType.EXPENSE }
            ManageUiState.Success(expenses, income)
        }
        .catch { emit(ManageUiState.Error(it.message ?: "Unknown error occurred")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ManageUiState.Loading
        )
}