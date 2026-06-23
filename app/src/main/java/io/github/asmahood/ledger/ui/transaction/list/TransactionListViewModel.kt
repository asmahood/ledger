package io.github.asmahood.ledger.ui.transaction.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.mapper.toDayGroups
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {
    val uiState: StateFlow<TransactionListUiState> = repository.getAllTransactionsStream()
        .map<List<Transaction>, TransactionListUiState> { transactions ->
            TransactionListUiState.Success(groups = transactions.toDayGroups())
        }
        .catch { emit(TransactionListUiState.Error(it.message ?: "Unknown error occurred")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionListUiState.Loading
        )
}