package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.PeriodTotals
import kotlin.math.roundToLong

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

private fun percentOfIncome(part: Double, income: Double): Int? =
    if (income == 0.0) null
    else (part / income * 100).roundToLong()
        .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
        .toInt()
