package io.github.asmahood.ledger.ui.transaction.list

import java.time.LocalDate

/**
 * Which header a day's transactions sit under. Kept as a result type rather than a finished
 * string so the date logic stays pure and testable; the composable maps each case to a string
 * resource ([DayHeader.Today] / [DayHeader.Yesterday]) or a formatted date ([DayHeader.On]).
 */
sealed interface DayHeader {
    data object Today : DayHeader
    data object Yesterday : DayHeader
    data class On(val date: LocalDate) : DayHeader
}

/** Resolve which header [date] belongs to. Pure: pass [today] in so it can be tested without a clock. */
fun dayHeaderFor(date: LocalDate, today: LocalDate): DayHeader = when (date) {
    today -> DayHeader.Today
    today.minusDays(1) -> DayHeader.Yesterday
    else -> DayHeader.On(date)
}
