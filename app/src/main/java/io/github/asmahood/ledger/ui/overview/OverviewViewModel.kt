package io.github.asmahood.ledger.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.projection.PeriodTotals
import io.github.asmahood.ledger.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _selectedPeriod = MutableStateFlow(OverviewPeriod.THIS_MONTH)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = selectedPeriod.flatMapLatest { period ->
        val range = period.toDateRange(LocalDate.now())
        transactionRepository.getPeriodTotalsStream(range.start, range.endInclusive)
    }.map<PeriodTotals, OverviewUiState> { totals ->
        OverviewUiState.Success(totals.toSummary())
    }.catch {
        emit(OverviewUiState.Error(it.message ?: "Unknown error occurred"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverviewUiState.Loading
    )


    fun onPeriodSelected(period: OverviewPeriod) {
        _selectedPeriod.value = period
    }
}