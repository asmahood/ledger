package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.CategoryMonthSpend
import io.github.asmahood.ledger.data.projection.MonthlyTotal
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

/** The inclusive list of months spanned by [start]..[end] — every chart's shared x-axis. */
private fun monthsInRange(start: LocalDate, end: LocalDate): List<YearMonth> =
    generateSequence(YearMonth.from(start)) { it.plusMonths(1) }
        .takeWhile { it <= YearMonth.from(end) }
        .toList()

fun List<CategoryMonthSpend>.toCategorySpendChart(
    start: LocalDate,
    end: LocalDate
): CategorySpendChart {
    val months = monthsInRange(start, end)
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

fun List<MonthlyTotal>.toTotalIncomeChart(start: LocalDate, end: LocalDate): TotalIncomeChart {
    val months = monthsInRange(start, end)
    val monthLabels = months.map { it.format(chartMonthLabel) }
    val incomeByMonth = associate { YearMonth.of(it.year, it.month) to it.total }
    return TotalIncomeChart(monthLabels, months.map { incomeByMonth[it] ?: 0.0 })
}

fun List<MonthlyTotal>.toTotalExpenseChart(start: LocalDate, end: LocalDate): TotalExpenseChart {
    val months = monthsInRange(start, end)
    val monthLabels = months.map { it.format(chartMonthLabel) }
    val expenseByMonth = associate { YearMonth.of(it.year, it.month) to it.total }
    return TotalExpenseChart(monthLabels, months.map { expenseByMonth[it] ?: 0.0 })
}

/**
 * Net savings per month = income − expense. Both charts are built over the same range and already
 * share an identical month axis, so this subtracts their aligned amounts rather than re-deriving
 * the month scaffold.
 */
fun TotalIncomeChart.toTotalSavingsChart(expense: TotalExpenseChart): TotalSavingsChart =
    TotalSavingsChart(
        monthLabels = monthLabels,
        amounts = amounts.zip(expense.amounts) { earned, spent -> earned - spent }
    )
