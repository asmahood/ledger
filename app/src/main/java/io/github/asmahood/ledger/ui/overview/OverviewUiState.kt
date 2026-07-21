package io.github.asmahood.ledger.ui.overview

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(
        val summary: OverviewSummary,
        val categorySpendChart: CategorySpendChart,
        val totalIncomeChart: TotalIncomeChart
    ) : OverviewUiState

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

data class CategorySpendChart(
    val monthLabels: List<String>,
    val series: List<CategorySeries>
)

data class CategorySeries(
    val categoryId: Long,
    val categoryName: String,
    val amounts: List<Double>,
    val isVisible: Boolean = true
)

data class TotalIncomeChart(
    val monthLabels: List<String>,
    val amounts: List<Double>
)