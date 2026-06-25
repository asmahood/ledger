package io.github.asmahood.ledger.ui.overview

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(val summary: OverviewSummary) : OverviewUiState
    data class Error(val message: String) : OverviewUiState
}

data class OverviewSummary(
    val income: Double,
    val expenses: Double,
    val saved: Double,
    val expensesPercentOfIncome: Int?,
    val savedPercentOfIncome: Int?,
) {
    val isNegativeSaved: Boolean get() = saved < 0
}
