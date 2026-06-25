package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.PeriodTotals
import kotlin.math.roundToInt

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

fun PeriodTotals.toSummary(): OverviewSummary {
    val income = income ?: 0.0
    val expenses = expenses ?: 0.0
    val saved = income - expenses
    return OverviewSummary(
        income = income,
        expenses = expenses,
        saved = saved,
        expensesPercentOfIncome = percentOfIncome(expenses, income),
        savedPercentOfIncome = percentOfIncome(saved, income)
    )
}

private fun percentOfIncome(part: Double, income: Double): Int? = if (income == 0.0) null else (part / income * 100).roundToInt()
