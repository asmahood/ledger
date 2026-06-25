package io.github.asmahood.ledger.ui.overview

import androidx.annotation.StringRes
import io.github.asmahood.ledger.R
import java.time.LocalDate
import java.time.YearMonth

enum class OverviewPeriod(@StringRes label: Int) {
    THIS_MONTH(R.string.period_this_month),
    LAST_MONTH(R.string.period_last_month),
    THREE_MONTHS(R.string.period_3_months),
    SIX_MONTHS(R.string.period_6_months),
    LAST_YEAR(R.string.period_last_year),
    YTD(R.string.period_ytd),
    ALL(R.string.period_all)
}

fun OverviewPeriod.toDateRange(today: LocalDate): ClosedRange<LocalDate> {
    val monthStart = today.withDayOfMonth(1)
    val monthEnd = YearMonth.from(today).atEndOfMonth()
    return when (this) {
        OverviewPeriod.THIS_MONTH -> monthStart..monthEnd
        OverviewPeriod.LAST_MONTH -> monthStart.minusMonths(1)..monthStart.minusDays(1)
        OverviewPeriod.THREE_MONTHS -> monthStart.minusMonths(2)..monthEnd
        OverviewPeriod.SIX_MONTHS -> monthStart.minusMonths(5)..monthEnd
        OverviewPeriod.LAST_YEAR -> monthStart.minusMonths(11)..monthEnd
        OverviewPeriod.YTD -> today.withDayOfYear(1)..today
        OverviewPeriod.ALL -> LocalDate.ofEpochDay(0)..today
    }
}