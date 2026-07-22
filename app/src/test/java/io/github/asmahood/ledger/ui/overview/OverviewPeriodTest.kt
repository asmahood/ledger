package io.github.asmahood.ledger.ui.overview

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OverviewPeriodTest {
    // Mid-month reference date so month-start/-end boundaries are unambiguous.
    private val today = LocalDate.of(2026, 6, 24)

    private fun assertRange(
        period: OverviewPeriod,
        today: LocalDate,
        expectedStart: LocalDate,
        expectedEnd: LocalDate,
        earliest: LocalDate? = null,
    ) {
        val range = period.toDateRange(today, earliest)
        assertEquals("start", expectedStart, range.start)
        assertEquals("end", expectedEnd, range.endInclusive)
    }

    @Test
    fun thisMonth_spansCurrentCalendarMonth() {
        assertRange(
            OverviewPeriod.THIS_MONTH, today,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
        )
    }

    @Test
    fun lastMonth_spansPreviousCalendarMonth() {
        assertRange(
            OverviewPeriod.LAST_MONTH, today,
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
        )
    }

    @Test
    fun threeMonths_includesCurrentMonthAndTwoPrior() {
        assertRange(
            OverviewPeriod.THREE_MONTHS, today,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30),
        )
    }

    @Test
    fun sixMonths_includesCurrentMonthAndFivePrior() {
        assertRange(
            OverviewPeriod.SIX_MONTHS, today,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
        )
    }

    @Test
    fun lastYear_isTrailingTwelveMonthsAlignedToMonths() {
        assertRange(
            OverviewPeriod.LAST_YEAR, today,
            LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30),
        )
    }

    @Test
    fun ytd_spansStartOfYearToToday() {
        assertRange(
            OverviewPeriod.YTD, today,
            LocalDate.of(2026, 1, 1), today,
        )
    }

    @Test
    fun all_startsAtEarliestTransaction() {
        assertRange(
            OverviewPeriod.ALL, today,
            LocalDate.of(2024, 3, 17), LocalDate.of(2026, 6, 30),
            earliest = LocalDate.of(2024, 3, 17),
        )
    }

    // With an empty database there is nothing to show; falling back to the current month keeps the
    // range to a single month instead of stretching back to the epoch.
    @Test
    fun all_withNoTransactions_fallsBackToCurrentMonth() {
        assertRange(
            OverviewPeriod.ALL, today,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
            earliest = null,
        )
    }

    @Test
    fun all_endsAtEndOfCurrentMonth_notToday() {
        val range = OverviewPeriod.ALL.toDateRange(today, LocalDate.of(2024, 3, 17))
        assertEquals(LocalDate.of(2026, 6, 30), range.endInclusive)
    }

    @Test
    fun periodsOtherThanAll_ignoreEarliestDate() {
        val ancient = LocalDate.of(2001, 1, 1)
        assertRange(
            OverviewPeriod.THREE_MONTHS, today,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30),
            earliest = ancient,
        )
    }

    @Test
    fun lastMonth_crossingYearBoundary_landsInPreviousYear() {
        val january = LocalDate.of(2026, 1, 15)
        assertRange(
            OverviewPeriod.LAST_MONTH, january,
            LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun sixMonths_crossingYearBoundary_walksBackThroughPreviousYear() {
        val february = LocalDate.of(2026, 2, 10)
        assertRange(
            OverviewPeriod.SIX_MONTHS, february,
            LocalDate.of(2025, 9, 1), LocalDate.of(2026, 2, 28),
        )
    }

    @Test
    fun ytd_onFirstDayOfYear_collapsesToSingleDay() {
        val newYear = LocalDate.of(2026, 1, 1)
        assertRange(OverviewPeriod.YTD, newYear, newYear, newYear)
    }

    @Test
    fun thisMonth_inLeapFebruary_endsOnTwentyNinth() {
        val leapFeb = LocalDate.of(2028, 2, 10)
        assertRange(
            OverviewPeriod.THIS_MONTH, leapFeb,
            LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29),
        )
    }
}
