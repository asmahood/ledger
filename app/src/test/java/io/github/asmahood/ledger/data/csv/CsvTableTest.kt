package io.github.asmahood.ledger.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTableTest {

    @Test
    fun parse_splitsHeadersAndRows() {
        val table = CsvTable.parse(
            """
            Date,Amount,Category
            2026-01-05,12.50,Groceries
            2026-01-06,4.25,Coffee
            """.trimIndent()
        )

        assertEquals(listOf("Date", "Amount", "Category"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("2026-01-05", "12.50", "Groceries"), table.rows[0])
    }

    @Test
    fun parse_keepsCommasInsideQuotedFields() {
        val table = CsvTable.parse(
            """
            Vendor,Amount
            "Loblaws, Bathurst St",42.18
            """.trimIndent()
        )

        assertEquals(listOf("Loblaws, Bathurst St", "42.18"), table.rows[0])
    }

    @Test
    fun parse_keepsNewlinesInsideQuotedFields() {
        val table = CsvTable.parse("Note,Amount\n\"line one\nline two\",5.00")

        assertEquals(listOf("line one\nline two", "5.00"), table.rows[0])
    }

    @Test
    fun parse_handlesCrlfLineEndings() {
        val table = CsvTable.parse("Date,Amount\r\n2026-01-05,12.50\r\n")

        assertEquals(listOf("Date", "Amount"), table.headers)
        assertEquals(listOf("2026-01-05", "12.50"), table.rows.single())
    }

    // Google Sheets writes a UTF-8 byte-order mark. Left in place it becomes part of the first
    // header name, and auto-detection silently fails to match the date column.
    @Test
    fun parse_stripsByteOrderMark() {
        val table = CsvTable.parse("﻿Date,Amount\n2026-01-05,12.50")

        assertEquals("Date", table.headers.first())
    }

    @Test
    fun parse_padsShortRowsToHeaderWidth() {
        val table = CsvTable.parse("Date,Amount,Note\n2026-01-05,12.50")

        assertEquals(listOf("2026-01-05", "12.50", ""), table.rows.single())
    }

    @Test
    fun parse_trimsRowsLongerThanTheHeader() {
        val table = CsvTable.parse("Date,Amount\n2026-01-05,12.50,extra")

        assertEquals(listOf("2026-01-05", "12.50"), table.rows.single())
    }

    @Test
    fun parse_ignoresBlankLines() {
        val table = CsvTable.parse("Date,Amount\n\n2026-01-05,12.50\n\n")

        assertEquals(1, table.rows.size)
    }

    @Test
    fun parse_emptyInput_yieldsEmptyTable() {
        val table = CsvTable.parse("")

        assertTrue(table.headers.isEmpty())
        assertTrue(table.rows.isEmpty())
    }

    @Test
    fun parse_headerOnly_yieldsNoRows() {
        val table = CsvTable.parse("Date,Amount,Category")

        assertEquals(3, table.headers.size)
        assertTrue(table.rows.isEmpty())
    }
}
