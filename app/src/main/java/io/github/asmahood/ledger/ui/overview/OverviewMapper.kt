package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.CategoryMonthSpend
import io.github.asmahood.ledger.data.projection.PeriodTotals
import io.github.asmahood.ledger.util.chartMonthLabel
import java.time.LocalDate
import java.time.YearMonth
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

fun List<CategoryMonthSpend>.toCategorySpendChart(start: LocalDate, end: LocalDate): CategorySpendChart {
    val months = generateSequence(YearMonth.from(start)) { it.plusMonths(1) }
        .takeWhile { it <= YearMonth.from(end) }
        .toList()
    val monthLabels = months.map { it.format(chartMonthLabel) }

    if (isEmpty()) return CategorySpendChart(monthLabels, emptyList())

    val series = groupBy { it.categoryId }.map { (_, rows) ->
        val spendByMonth = rows.associate { YearMonth.of(it.year, it.month) to it.total }
        CategorySeries(
            categoryId = rows.first().categoryId,
            categoryName = rows.first().categoryName,
            amounts = months.map { spendByMonth[it] ?: 0.0 }
        )
    }.sortedBy { it.categoryName }

    return CategorySpendChart(monthLabels, series)
}