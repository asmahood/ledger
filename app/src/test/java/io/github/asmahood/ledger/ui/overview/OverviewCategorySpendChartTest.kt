package io.github.asmahood.ledger.ui.overview

import io.github.asmahood.ledger.data.projection.CategoryMonthSpend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OverviewCategorySpendChartTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val end = LocalDate.of(2026, 3, 31)

    private fun spend(
        categoryId: Long,
        categoryName: String,
        year: Int = 2026,
        month: Int,
        total: Double,
    ) = CategoryMonthSpend(
        categoryId = categoryId,
        categoryName = categoryName,
        year = year,
        month = month,
        total = total,
    )

    @Test
    fun toCategorySpendChart_buildsInclusiveMonthLabelsAcrossRange() {
        val chart = emptyList<CategoryMonthSpend>().toCategorySpendChart(start, end)

        // Jan, Feb and Mar are all included; labels use the "MMM 'yy" format.
        assertEquals(listOf("Jan '26", "Feb '26", "Mar '26"), chart.monthLabels)
    }

    @Test
    fun toCategorySpendChart_singleMonthRange_hasOneLabel() {
        val january = LocalDate.of(2026, 1, 15)
        val chart = emptyList<CategoryMonthSpend>().toCategorySpendChart(january, january)

        assertEquals(listOf("Jan '26"), chart.monthLabels)
    }

    @Test
    fun toCategorySpendChart_emptySpend_hasLabelsButNoSeries() {
        val chart = emptyList<CategoryMonthSpend>().toCategorySpendChart(start, end)

        assertEquals(3, chart.monthLabels.size)
        assertTrue(chart.series.isEmpty())
    }

    @Test
    fun toCategorySpendChart_fillsMissingMonthsWithZero() {
        // Gas only has spend in January and March; February must be zero-filled.
        val chart = listOf(
            spend(categoryId = 1, categoryName = "Gas", month = 1, total = 56.0),
            spend(categoryId = 1, categoryName = "Gas", month = 3, total = 73.22),
        ).toCategorySpendChart(start, end)

        val gas = chart.series.single()
        assertEquals(listOf(56.0, 0.0, 73.22), gas.amounts)
    }

    @Test
    fun toCategorySpendChart_alignsAmountsToMonthOrder() {
        val chart = listOf(
            spend(categoryId = 2, categoryName = "Rent", month = 1, total = 1500.0),
            spend(categoryId = 2, categoryName = "Rent", month = 2, total = 1500.0),
            spend(categoryId = 2, categoryName = "Rent", month = 3, total = 1500.0),
        ).toCategorySpendChart(start, end)

        val rent = chart.series.single()
        assertEquals(listOf(1500.0, 1500.0, 1500.0), rent.amounts)
    }

    @Test
    fun toCategorySpendChart_sortsSeriesByCategoryName() {
        // Input order is Rent before Gas; output must be alphabetical.
        val chart = listOf(
            spend(categoryId = 2, categoryName = "Rent", month = 1, total = 1500.0),
            spend(categoryId = 1, categoryName = "Gas", month = 1, total = 56.0),
        ).toCategorySpendChart(start, end)

        assertEquals(listOf("Gas", "Rent"), chart.series.map { it.categoryName })
    }

    @Test
    fun toCategorySpendChart_groupsRowsByCategory() {
        val chart = listOf(
            spend(categoryId = 1, categoryName = "Gas", month = 1, total = 56.0),
            spend(categoryId = 1, categoryName = "Gas", month = 2, total = 45.85),
            spend(categoryId = 2, categoryName = "Rent", month = 1, total = 1500.0),
        ).toCategorySpendChart(start, end)

        assertEquals(2, chart.series.size)
        assertEquals(setOf(1L, 2L), chart.series.map { it.categoryId }.toSet())
    }

    @Test
    fun toCategorySpendChart_seriesAreVisibleByDefault() {
        val chart = listOf(
            spend(categoryId = 1, categoryName = "Gas", month = 1, total = 56.0),
        ).toCategorySpendChart(start, end)

        assertTrue(chart.series.all { it.isVisible })
    }

    @Test
    fun toCategorySpendChart_spanningYearBoundary_labelsCarryTheYear() {
        val chart = emptyList<CategoryMonthSpend>().toCategorySpendChart(
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2026, 1, 31),
        )

        assertEquals(listOf("Dec '25", "Jan '26"), chart.monthLabels)
    }
}
