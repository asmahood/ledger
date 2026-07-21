package io.github.asmahood.ledger.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * The savings chart's tap marker (AC2) renders the net amount through [formatCurrency], and net
 * savings is the first place a negative amount reaches it. The marker paints onto the chart canvas
 * rather than the Compose semantics tree, so it can't be asserted in a UI test — this locks in the
 * label content, including the negative path, at the unit level. Locale is pinned so the expected
 * strings are deterministic regardless of the host's default.
 */
class FormatTest {
    private lateinit var original: Locale

    @Before
    fun setUp() {
        original = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(original)
    }

    @Test
    fun formatCurrency_negativeAmount_showsLeadingMinusAndDollar() {
        assertEquals("-$1,100.00", formatCurrency(-1100.0))
    }

    @Test
    fun formatCurrency_positiveAmount_hasNoSign() {
        assertEquals("$1,100.00", formatCurrency(1100.0))
    }

    @Test
    fun formatCurrency_zero_rendersZeroDollars() {
        assertEquals("$0.00", formatCurrency(0.0))
    }
}
