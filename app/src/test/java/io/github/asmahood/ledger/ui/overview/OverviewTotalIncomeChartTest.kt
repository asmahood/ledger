package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.MonthlyTotal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OverviewTotalIncomeChartTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val end = LocalDate.of(2026, 3, 31)

    private fun total(year: Int = 2026, month: Int, total: Double) =
        MonthlyTotal(year = year, month = month, total = total)

    @Test
    fun toTotalIncomeChart_buildsInclusiveMonthLabelsAcrossRange() {
        val chart = emptyList<MonthlyTotal>().toTotalIncomeChart(start, end)

        // Jan, Feb and Mar are all included; labels use the "MMM 'yy" format.
        assertEquals(listOf("Jan '26", "Feb '26", "Mar '26"), chart.monthLabels)
    }

    @Test
    fun toTotalIncomeChart_singleMonthRange_hasOneLabel() {
        val january = LocalDate.of(2026, 1, 15)
        val chart = emptyList<MonthlyTotal>().toTotalIncomeChart(january, january)

        assertEquals(listOf("Jan '26"), chart.monthLabels)
    }

    @Test
    fun toTotalIncomeChart_emptyInput_amountsAreZeroAndParallelToLabels() {
        val chart = emptyList<MonthlyTotal>().toTotalIncomeChart(start, end)

        assertEquals(3, chart.monthLabels.size)
        assertEquals(chart.monthLabels.size, chart.amounts.size)
        assertEquals(listOf(0.0, 0.0, 0.0), chart.amounts)
    }

    @Test
    fun toTotalIncomeChart_fillsMissingMonthsWithZero() {
        // Income only recorded in January and March; February must be zero-filled (AC4).
        val chart = listOf(
            total(month = 1, total = 4000.0),
            total(month = 3, total = 4200.0),
        ).toTotalIncomeChart(start, end)

        assertEquals(listOf(4000.0, 0.0, 4200.0), chart.amounts)
    }

    @Test
    fun toTotalIncomeChart_alignsAmountsToMonthOrder() {
        val chart = listOf(
            total(month = 1, total = 4000.0),
            total(month = 2, total = 3950.0),
            total(month = 3, total = 4600.0),
        ).toTotalIncomeChart(start, end)

        assertEquals(listOf(4000.0, 3950.0, 4600.0), chart.amounts)
    }

    @Test
    fun toTotalIncomeChart_amountsAreAlwaysParallelToMonthLabels() {
        val chart = listOf(
            total(month = 2, total = 3950.0),
        ).toTotalIncomeChart(start, end)

        assertEquals(chart.monthLabels.size, chart.amounts.size)
    }

    @Test
    fun toTotalIncomeChart_spanningYearBoundary_labelsCarryTheYear() {
        val chart = emptyList<MonthlyTotal>().toTotalIncomeChart(
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2026, 1, 31),
        )

        assertEquals(listOf("Dec '25", "Jan '26"), chart.monthLabels)
    }

    @Test
    fun toTotalIncomeChart_spanningYearBoundary_alignsAmountsToMonthOrder() {
        val chart = listOf(
            total(year = 2025, month = 12, total = 3800.0),
            total(year = 2026, month = 1, total = 4000.0),
        ).toTotalIncomeChart(
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2026, 1, 31),
        )

        assertEquals(listOf(3800.0, 4000.0), chart.amounts)
    }
}
