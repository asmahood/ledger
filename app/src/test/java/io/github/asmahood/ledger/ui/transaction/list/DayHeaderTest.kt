package io.github.asmahood.ledger.ui.transaction.list

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayHeaderTest {
    private val today = LocalDate.of(2026, 3, 12)

    @Test
    fun dayHeaderFor_sameDate_isToday() {
        assertEquals(DayHeader.Today, dayHeaderFor(today, today))
    }

    @Test
    fun dayHeaderFor_dayBefore_isYesterday() {
        assertEquals(DayHeader.Yesterday, dayHeaderFor(today.minusDays(1), today))
    }

    @Test
    fun dayHeaderFor_olderDate_isOnThatDate() {
        val older = today.minusDays(2)
        assertEquals(DayHeader.On(older), dayHeaderFor(older, today))
    }

    @Test
    fun dayHeaderFor_futureDate_isOnThatDate() {
        // Not expected in practice, but the date branch should still own anything that isn't
        // today or yesterday rather than silently falling through.
        val future = today.plusDays(1)
        assertEquals(DayHeader.On(future), dayHeaderFor(future, today))
    }
}
