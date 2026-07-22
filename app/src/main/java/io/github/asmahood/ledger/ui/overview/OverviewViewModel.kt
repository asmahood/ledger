package io.github.asmahood.ledger.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _selectedPeriod = MutableStateFlow(OverviewPeriod.THIS_MONTH)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _hiddenCategoryIds = MutableStateFlow<Set<Long>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _periodData = combine(
        selectedPeriod,
        transactionRepository.getEarliestTransactionDateStream()
    ) { period, earliest -> period to earliest }.flatMapLatest { (period, earliest) ->
        val range = period.toDateRange(LocalDate.now(), earliest)
        combine(
            transactionRepository.getPeriodTotalsStream(range.start, range.endInclusive),
            transactionRepository.getMonthlyCategoryTotalsStream(range.start, range.endInclusive),
            transactionRepository.getMonthlyTotalsByTypeStream(
                TransactionType.INCOME,
                range.start,
                range.endInclusive
            ),
            transactionRepository.getMonthlyTotalsByTypeStream(
                TransactionType.EXPENSE,
                range.start,
                range.endInclusive
            )
        ) { totals, categorySpend, totalIncome, totalExpense ->
            val incomeChart = totalIncome.toTotalIncomeChart(range.start, range.endInclusive)
            val expenseChart = totalExpense.toTotalExpenseChart(range.start, range.endInclusive)
            PeriodData(
                summary = totals.toSummary(),
                categorySpendChart = categorySpend.toCategorySpendChart(
                    range.start,
                    range.endInclusive
                ),
                totalIncomeChart = incomeChart,
                totalExpenseChart = expenseChart,
                totalSavingsChart = incomeChart.toTotalSavingsChart(expenseChart)
            )
        }
    }

    val uiState = combine(_periodData, _hiddenCategoryIds) { data, hiddenIds ->
        OverviewUiState.Success(
            summary = data.summary,
            categorySpendChart = data.categorySpendChart.copy(
                series = data.categorySpendChart.series.map { it.copy(isVisible = it.categoryId !in hiddenIds) }
            ),
            totalIncomeChart = data.totalIncomeChart,
            totalExpenseChart = data.totalExpenseChart,
            totalSavingsChart = data.totalSavingsChart
        ) as OverviewUiState
    }.catch {
        emit(OverviewUiState.Error(it.message ?: "Unknown error occurred"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverviewUiState.Loading
    )

    /** Building the summary and chart is done once per period; toggling visibility only re-copies. */
    private data class PeriodData(
        val summary: OverviewSummary,
        val categorySpendChart: CategorySpendChart,
        val totalIncomeChart: TotalIncomeChart,
        val totalExpenseChart: TotalExpenseChart,
        val totalSavingsChart: TotalSavingsChart
    )

    fun onPeriodSelected(period: OverviewPeriod) {
        // A hidden category from the previous period shouldn't silently stay hidden in the next one.
        _hiddenCategoryIds.value = emptySet()
        _selectedPeriod.value = period
    }

    fun onCategoryToggled(categoryId: Long) {
        _hiddenCategoryIds.update { current ->
            if (categoryId in current) current - categoryId else current + categoryId
        }
    }
}