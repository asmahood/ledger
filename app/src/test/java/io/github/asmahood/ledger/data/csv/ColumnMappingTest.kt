package io.github.asmahood.ledger.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColumnMappingTest {

    @Test
    fun normalize_lowercasesAndStripsPunctuationAndSpaces() {
        assertEquals("transactiondate", ColumnMapping.normalize(" Transaction Date "))
        assertEquals("txndate", ColumnMapping.normalize("txn_date"))
        assertEquals("amount", ColumnMapping.normalize("AMOUNT"))
    }

    @Test
    fun autoDetect_matchesExactHeaderNames() {
        val mapping = ColumnMapping.autoDetect(
            listOf("Date", "Amount", "Category", "Vendor", "Type", "Note")
        )

        assertEquals(0, mapping[ImportField.DATE])
        assertEquals(1, mapping[ImportField.AMOUNT])
        assertEquals(2, mapping[ImportField.CATEGORY])
        assertEquals(3, mapping[ImportField.VENDOR])
        assertEquals(4, mapping[ImportField.TYPE])
        assertEquals(5, mapping[ImportField.NOTE])
    }

    @Test
    fun autoDetect_matchesSynonyms() {
        val mapping = ColumnMapping.autoDetect(listOf("txn_date", "Merchant", "Memo", "Total"))

        assertEquals(0, mapping[ImportField.DATE])
        assertEquals(1, mapping[ImportField.VENDOR])
        assertEquals(2, mapping[ImportField.NOTE])
        assertEquals(3, mapping[ImportField.AMOUNT])
    }

    @Test
    fun autoDetect_leavesUnknownHeadersUnmapped() {
        val mapping = ColumnMapping.autoDetect(listOf("Date", "Balance", "Cheque #"))

        assertEquals(0, mapping[ImportField.DATE])
        assertNull(mapping[ImportField.AMOUNT])
        assertNull(mapping[ImportField.CATEGORY])
    }

    // Two columns both matching one field would otherwise both be claimed, and the second would
    // silently overwrite the first. First column wins; the user can override in the UI.
    @Test
    fun autoDetect_claimsEachColumnOnce() {
        val mapping = ColumnMapping.autoDetect(listOf("Note", "Memo"))

        assertEquals(0, mapping[ImportField.NOTE])
    }

    @Test
    fun autoDetect_emptyHeaders_yieldsEmptyMapping() {
        assertEquals(ColumnMapping.EMPTY, ColumnMapping.autoDetect(emptyList()))
    }

    @Test
    fun isComplete_requiresDateAmountAndCategory() {
        val mapping = ColumnMapping.autoDetect(listOf("Date", "Amount", "Category"))

        assertTrue(mapping.isComplete)
    }

    @Test
    fun isComplete_falseWhenARequiredFieldIsUnmapped() {
        val mapping = ColumnMapping.autoDetect(listOf("Date", "Amount"))

        assertFalse(mapping.isComplete)
    }

    @Test
    fun isComplete_doesNotRequireOptionalFields() {
        val mapping = ColumnMapping.autoDetect(listOf("Date", "Amount", "Category"))

        assertNull(mapping[ImportField.VENDOR])
        assertNull(mapping[ImportField.TYPE])
        assertNull(mapping[ImportField.NOTE])
        assertTrue(mapping.isComplete)
    }

    @Test
    fun with_setsAColumn() {
        val mapping = ColumnMapping.EMPTY.with(ImportField.DATE, 3)

        assertEquals(3, mapping[ImportField.DATE])
    }

    @Test
    fun with_nullClearsAColumn() {
        val mapping = ColumnMapping.EMPTY.with(ImportField.DATE, 3).with(ImportField.DATE, null)

        assertNull(mapping[ImportField.DATE])
        assertFalse(mapping.isComplete)
    }

    @Test
    fun with_replacesAnExistingColumn() {
        val mapping = ColumnMapping.EMPTY.with(ImportField.AMOUNT, 1).with(ImportField.AMOUNT, 4)

        assertEquals(4, mapping[ImportField.AMOUNT])
    }
}
