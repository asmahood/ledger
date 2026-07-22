package io.github.asmahood.ledger.ui.overview

import kotlin.math.roundToLong

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(
        val summary: OverviewSummary,
        val categorySpendChart: CategorySpendChart,
        val totalIncomeChart: TotalIncomeChart,
        val totalExpenseChart: TotalExpenseChart,
        val totalSavingsChart: TotalSavingsChart,
        val budgetSummary: BudgetSummary,
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

data class TotalExpenseChart(
    val monthLabels: List<String>,
    val amounts: List<Double>
)

data class TotalSavingsChart(
    val monthLabels: List<String>,
    val amounts: List<Double>
)

enum class BudgetStatus { UNDER, NEAR, OVER, UNBUDGETED }

data class BudgetRow(
    val label: String,
    val actual: Double,
    val target: Double,
    val percent: Int?,
    val status: BudgetStatus,
) {
    /** How much of the bar to paint. A non-positive target has no room in it at all. */
    val fraction: Float
        get() =
            if (target <= 0.0) (if (actual > 0.0) 1f else 0f)
            else (actual / target).coerceIn(0.0, 1.0).toFloat()

    companion object {
        /**
         * Derives the percentage, status and bar fill from a pair of amounts, so the card renders
         * what it is given rather than deciding anything itself.
         */
        fun of(label: String, actual: Double, target: Double, unbudgeted: Boolean): BudgetRow {
            val percent =
                if (target <= 0.0) null
                else (actual / target * 100).roundToLong()
                    .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                    .toInt()

            // Overflow outranks everything: an unbudgeted category that has eaten through the
            // implied-savings pool reads red, not its usual blue.
            val status = when {
                actual > 0.0 && (percent == null || percent > 100) -> BudgetStatus.OVER
                unbudgeted -> BudgetStatus.UNBUDGETED
                percent != null && percent > 80 -> BudgetStatus.NEAR
                else -> BudgetStatus.UNDER
            }

            return BudgetRow(
                label = label,
                actual = actual,
                target = target,
                percent = percent,
                status = status,
            )
        }
    }
}

data class BudgetSummary(
    val budgeted: List<BudgetRow>,
    val budgetedTotal: BudgetRow,
    val unbudgeted: List<BudgetRow>,
    val unbudgetedTotal: BudgetRow,
)