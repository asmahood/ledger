package io.github.asmahood.ledger.data.csv

import io.github.asmahood.ledger.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RowParserTest {

    // Date, Amount, Category, Vendor, Type, Note — the fully-mapped six-column shape.
    private val fullMapping = ColumnMapping.EMPTY
        .with(ImportField.DATE, 0)
        .with(ImportField.AMOUNT, 1)
        .with(ImportField.CATEGORY, 2)
        .with(ImportField.VENDOR, 3)
        .with(ImportField.TYPE, 4)
        .with(ImportField.NOTE, 5)

    private fun parse(vararg cells: String, mapping: ColumnMapping = fullMapping) =
        RowParser.parse(cells.toList(), mapping, lineNumber = 2)

    private fun valid(vararg cells: String, mapping: ColumnMapping = fullMapping) =
        parse(*cells, mapping = mapping) as ParsedRow.Valid

    private fun invalid(vararg cells: String, mapping: ColumnMapping = fullMapping) =
        parse(*cells, mapping = mapping) as ParsedRow.Invalid

    // --- dates ---

    @Test
    fun parseDate_acceptsIsoFormat() {
        assertEquals(LocalDate.of(2026, 1, 5), RowParser.parseDate("2026-01-05"))
    }

    @Test
    fun parseDate_acceptsMonthFirstSlashFormat() {
        assertEquals(LocalDate.of(2026, 1, 5), RowParser.parseDate("1/5/2026"))
        assertEquals(LocalDate.of(2026, 12, 31), RowParser.parseDate("12/31/2026"))
    }

    @Test
    fun parseDate_acceptsIsoSlashFormat() {
        assertEquals(LocalDate.of(2026, 1, 5), RowParser.parseDate("2026/01/05"))
    }

    @Test
    fun parseDate_acceptsMonthFirstDashFormat() {
        assertEquals(LocalDate.of(2026, 1, 5), RowParser.parseDate("1-5-2026"))
        assertEquals(LocalDate.of(2026, 1, 31), RowParser.parseDate("01-31-2026"))
        assertEquals(LocalDate.of(2026, 12, 31), RowParser.parseDate("12-31-2026"))
    }

    // A four-digit leading field is a year, never a month, so the ISO pattern must win the race.
    @Test
    fun parseDate_prefersIsoOverMonthFirstForYearLeadingDates() {
        assertEquals(LocalDate.of(2026, 1, 5), RowParser.parseDate("2026-01-05"))
    }

    @Test
    fun parseDate_rejectsDayFirstDashFormat() {
        assertNull(RowParser.parseDate("31-12-2026"))
    }

    @Test
    fun parseDate_trimsSurroundingWhitespace() {
        assertEquals(LocalDate.of(2026, 1, 5), RowParser.parseDate("  2026-01-05 "))
    }

    // Day-first is indistinguishable from month-first for days 1-12, so accepting it would import
    // silently wrong dates. Rejecting it makes the failure visible in the preview instead.
    @Test
    fun parseDate_rejectsDayFirstFormat() {
        assertNull(RowParser.parseDate("31/12/2026"))
    }

    @Test
    fun parseDate_rejectsImpossibleDates() {
        assertNull(RowParser.parseDate("2026-02-30"))
        assertNull(RowParser.parseDate("13/1/2026"))
    }

    @Test
    fun parseDate_rejectsGarbage() {
        assertNull(RowParser.parseDate("last tuesday"))
        assertNull(RowParser.parseDate(""))
    }

    // --- amounts ---

    @Test
    fun parseAmount_readsPlainNumbers() {
        assertEquals(12.50, RowParser.parseAmount("12.50")!!, 0.001)
    }

    @Test
    fun parseAmount_stripsCurrencySymbolsAndSpaces() {
        assertEquals(12.50, RowParser.parseAmount(" $12.50 ")!!, 0.001)
    }

    @Test
    fun parseAmount_stripsThousandsSeparators() {
        assertEquals(1234.56, RowParser.parseAmount("$1,234.56")!!, 0.001)
    }

    @Test
    fun parseAmount_readsLeadingMinusAsNegative() {
        assertEquals(-12.50, RowParser.parseAmount("-12.50")!!, 0.001)
    }

    @Test
    fun parseAmount_readsParenthesesAsNegative() {
        assertEquals(-12.50, RowParser.parseAmount("($12.50)")!!, 0.001)
    }

    @Test
    fun parseAmount_rejectsNonNumbers() {
        assertNull(RowParser.parseAmount("n/a"))
        assertNull(RowParser.parseAmount(""))
        assertNull(RowParser.parseAmount("$"))
    }

    // --- types ---

    @Test
    fun parseType_readsKnownWordsCaseInsensitively() {
        assertEquals(TransactionType.EXPENSE, RowParser.parseType("Expense"))
        assertEquals(TransactionType.EXPENSE, RowParser.parseType("DEBIT"))
        assertEquals(TransactionType.INCOME, RowParser.parseType("income"))
        assertEquals(TransactionType.INCOME, RowParser.parseType(" Credit "))
    }

    @Test
    fun parseType_rejectsUnknownWords() {
        assertNull(RowParser.parseType("transfer"))
    }

    // --- whole rows ---

    @Test
    fun parse_validRow_mapsEveryField() {
        val row = valid("2026-01-05", "12.50", "Groceries", "Food Basics", "Expense", "weekly shop")

        assertEquals(2, row.lineNumber)
        assertEquals(LocalDate.of(2026, 1, 5), row.date)
        assertEquals(12.50, row.amount, 0.001)
        assertEquals("Groceries", row.categoryName)
        assertEquals("Food Basics", row.vendor)
        assertEquals(TransactionType.EXPENSE, row.type)
        assertEquals("weekly shop", row.note)
    }

    // Sign lives in TransactionType, never in the stored amount — the rest of the app assumes
    // amounts are positive and derives sign from type.
    @Test
    fun parse_negativeAmount_isStoredAbsolute() {
        val row = valid("2026-01-05", "-12.50", "Groceries", "Food Basics", "Expense", "")

        assertEquals(12.50, row.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, row.type)
    }

    @Test
    fun parse_typeColumnWins_overAmountSign() {
        val row = valid("2026-01-05", "-1500.00", "Salary", "Employer", "Income", "")

        assertEquals(TransactionType.INCOME, row.type)
        assertEquals(1500.00, row.amount, 0.001)
    }

    @Test
    fun parse_typeUnmapped_negativeAmountIsExpense() {
        val mapping = fullMapping.with(ImportField.TYPE, null)
        val row = valid("2026-01-05", "-12.50", "Groceries", "Food Basics", "", "", mapping = mapping)

        assertEquals(TransactionType.EXPENSE, row.type)
    }

    @Test
    fun parse_typeUnmapped_positiveAmountIsIncome() {
        val mapping = fullMapping.with(ImportField.TYPE, null)
        val row = valid("2026-01-05", "1500.00", "Salary", "Employer", "", "", mapping = mapping)

        assertEquals(TransactionType.INCOME, row.type)
    }

    @Test
    fun parse_blankVendor_becomesEmptyString() {
        val row = valid("2026-01-05", "12.50", "Groceries", "  ", "Expense", "")

        assertEquals("", row.vendor)
    }

    @Test
    fun parse_vendorUnmapped_becomesEmptyString() {
        val mapping = fullMapping.with(ImportField.VENDOR, null)
        val row = valid("2026-01-05", "12.50", "Groceries", "", "Expense", "", mapping = mapping)

        assertEquals("", row.vendor)
    }

    @Test
    fun parse_blankNote_becomesNull() {
        val row = valid("2026-01-05", "12.50", "Groceries", "Food Basics", "Expense", "   ")

        assertNull(row.note)
    }

    @Test
    fun parse_trimsCategoryName() {
        val row = valid("2026-01-05", "12.50", "  Groceries ", "Food Basics", "Expense", "")

        assertEquals("Groceries", row.categoryName)
    }

    @Test
    fun parse_shortRow_treatsMissingCellsAsBlank() {
        val row = invalid("2026-01-05", "12.50")

        assertEquals(RowError.MISSING_CATEGORY, row.error)
    }

    // --- invalid rows ---

    @Test
    fun parse_blankDate_isMissingDate() {
        assertEquals(
            RowError.MISSING_DATE,
            invalid("", "12.50", "Groceries", "Food Basics", "Expense", "").error,
        )
    }

    @Test
    fun parse_unreadableDate_isUnparseableDate() {
        assertEquals(
            RowError.UNPARSEABLE_DATE,
            invalid("31/12/2026", "12.50", "Groceries", "Food Basics", "Expense", "").error,
        )
    }

    @Test
    fun parse_blankAmount_isMissingAmount() {
        assertEquals(
            RowError.MISSING_AMOUNT,
            invalid("2026-01-05", "", "Groceries", "Food Basics", "Expense", "").error,
        )
    }

    @Test
    fun parse_unreadableAmount_isUnparseableAmount() {
        assertEquals(
            RowError.UNPARSEABLE_AMOUNT,
            invalid("2026-01-05", "n/a", "Groceries", "Food Basics", "Expense", "").error,
        )
    }

    @Test
    fun parse_zeroAmount_isZeroAmount() {
        assertEquals(
            RowError.ZERO_AMOUNT,
            invalid("2026-01-05", "0.00", "Groceries", "Food Basics", "Expense", "").error,
        )
    }

    @Test
    fun parse_blankCategory_isMissingCategory() {
        assertEquals(
            RowError.MISSING_CATEGORY,
            invalid("2026-01-05", "12.50", "", "Food Basics", "Expense", "").error,
        )
    }

    @Test
    fun parse_unknownType_isUnknownType() {
        assertEquals(
            RowError.UNKNOWN_TYPE,
            invalid("2026-01-05", "12.50", "Groceries", "Food Basics", "transfer", "").error,
        )
    }

    // A blank cell in a mapped Type column is not an error — fall back to the amount's sign, the
    // same rule as an unmapped Type column.
    @Test
    fun parse_blankType_fallsBackToAmountSign() {
        val row = valid("2026-01-05", "-12.50", "Groceries", "Food Basics", "", "")

        assertEquals(TransactionType.EXPENSE, row.type)
    }

    @Test
    fun parse_invalidRow_keepsRawCellsForDisplay() {
        val cells = listOf("nope", "12.50", "Groceries", "Food Basics", "Expense", "")
        val row = RowParser.parse(cells, fullMapping, lineNumber = 7) as ParsedRow.Invalid

        assertEquals(7, row.lineNumber)
        assertEquals(cells, row.raw)
    }

    // Date is checked before amount, amount before category — so a row broken in several ways
    // reports one stable, predictable reason rather than whichever check happened to run first.
    @Test
    fun parse_multipleProblems_reportsTheFirstInFieldOrder() {
        assertEquals(
            RowError.MISSING_DATE,
            invalid("", "", "", "Food Basics", "Expense", "").error,
        )
    }
}
