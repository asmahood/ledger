package io.github.asmahood.ledger.data.csv

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class ImportPlannerTest {

    private val jan5: LocalDate = LocalDate.of(2026, 1, 5)

    private fun row(
        lineNumber: Int,
        categoryName: String = "Groceries",
        amount: Double = 12.50,
        date: LocalDate = jan5,
        vendor: String = "Food Basics",
        type: TransactionType = TransactionType.EXPENSE,
    ) = ParsedRow.Valid(lineNumber, date, amount, categoryName, vendor, type, null)

    private fun category(id: Long, name: String, type: TransactionType = TransactionType.EXPENSE) =
        Category(id, name, type)

    private fun transaction(
        id: Long,
        category: Category,
        amount: Double = 12.50,
        date: LocalDate = jan5,
        vendor: String = "Food Basics",
    ) = Transaction(id, amount, date, vendor, category.type, null, category)

    // --- unmatched categories ---

    @Test
    fun unmatchedCategoryNames_findsNamesWithNoExistingCategory() {
        val names = ImportPlanner.unmatchedCategoryNames(
            rows = listOf(row(2, "Groceries"), row(3, "Coffee")),
            existing = listOf(category(1, "Groceries")),
        )

        assertEquals(listOf("Coffee"), names)
    }

    @Test
    fun unmatchedCategoryNames_matchesCaseInsensitively() {
        val names = ImportPlanner.unmatchedCategoryNames(
            rows = listOf(row(2, "groceries")),
            existing = listOf(category(1, "Groceries")),
        )

        assertTrue(names.isEmpty())
    }

    // A category saved in the app with a stray trailing space is the same category as the CSV's
    // spelling of it. Matching on case alone leaves it permanently unmatched, forcing the user to
    // re-resolve it on every import.
    @Test
    fun unmatchedCategoryNames_ignoresSurroundingWhitespaceOnTheExistingName() {
        val names = ImportPlanner.unmatchedCategoryNames(
            rows = listOf(row(2, "Restaurant")),
            existing = listOf(category(1, "Restaurant ")),
        )

        assertTrue(names.isEmpty())
    }

    @Test
    fun duplicateKey_ignoresSurroundingWhitespace() {
        assertEquals(
            ImportPlanner.duplicateKey(jan5, 12.50, "Restaurant ", " Food Basics"),
            ImportPlanner.duplicateKey(jan5, 12.50, "Restaurant", "Food Basics"),
        )
    }

    @Test
    fun unmatchedCategoryNames_deduplicates() {
        val names = ImportPlanner.unmatchedCategoryNames(
            rows = listOf(row(2, "Coffee"), row(3, "Coffee"), row(4, "coffee")),
            existing = emptyList(),
        )

        assertEquals(listOf("Coffee"), names)
    }

    @Test
    fun unmatchedCategoryNames_sortsAlphabetically() {
        val names = ImportPlanner.unmatchedCategoryNames(
            rows = listOf(row(2, "Transport"), row(3, "Coffee"), row(4, "Rent")),
            existing = emptyList(),
        )

        assertEquals(listOf("Coffee", "Rent", "Transport"), names)
    }

    @Test
    fun unmatchedCategoryNames_ignoresInvalidRows() {
        val names = ImportPlanner.unmatchedCategoryNames(
            rows = listOf(ParsedRow.Invalid(2, RowError.MISSING_DATE, listOf("", "12.50", "Coffee"))),
            existing = emptyList(),
        )

        assertTrue(names.isEmpty())
    }

    // --- default type for a new category ---

    @Test
    fun defaultTypeFor_usesTheTypeOfTheRowsUsingIt() {
        val type = ImportPlanner.defaultTypeFor(
            name = "Salary",
            rows = listOf(row(2, "Salary", type = TransactionType.INCOME)),
        )

        assertEquals(TransactionType.INCOME, type)
    }

    // Rows disagreeing about a category's type is a data problem the user must resolve; expense is
    // the safer default because it is by far the commoner case.
    @Test
    fun defaultTypeFor_disagreeingRows_defaultsToExpense() {
        val type = ImportPlanner.defaultTypeFor(
            name = "Refunds",
            rows = listOf(
                row(2, "Refunds", type = TransactionType.INCOME),
                row(3, "Refunds", type = TransactionType.EXPENSE),
            ),
        )

        assertEquals(TransactionType.EXPENSE, type)
    }

    @Test
    fun defaultTypeFor_noMatchingRows_defaultsToExpense() {
        assertEquals(TransactionType.EXPENSE, ImportPlanner.defaultTypeFor("Nothing", emptyList()))
    }

    // --- duplicates ---

    // A CSV category mapped onto an existing category with a different spelling ("Restaurant" in
    // the file, "Restaurants" in the app) is stored under the app's name. Keying duplicates on the
    // CSV's spelling would make every such row invisible to detection on re-import.
    @Test
    fun duplicateLineNumbers_usesTheNameTheRowWillBeStoredUnder() {
        val restaurants = category(1, "Restaurants")
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(row(2, categoryName = "Restaurant")),
            existing = listOf(transaction(1, restaurants)),
            categoryNameFor = { restaurants.name },
        )

        assertEquals(setOf(2), lines)
    }

    @Test
    fun duplicateLineNumbers_defaultsToTheCsvCategoryName() {
        val groceries = category(1, "Groceries")
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(row(2, categoryName = "Groceries")),
            existing = listOf(transaction(1, groceries)),
        )

        assertEquals(setOf(2), lines)
    }

    @Test
    fun duplicateLineNumbers_flagsRowsMatchingAnExistingTransaction() {
        val groceries = category(1, "Groceries")
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(row(2), row(3, amount = 99.99)),
            existing = listOf(transaction(1, groceries)),
        )

        assertEquals(setOf(2), lines)
    }

    @Test
    fun duplicateLineNumbers_comparesVendorCaseInsensitively() {
        val groceries = category(1, "Groceries")
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(row(2, vendor = "FOOD BASICS")),
            existing = listOf(transaction(1, groceries, vendor = "Food Basics")),
        )

        assertEquals(setOf(2), lines)
    }

    @Test
    fun duplicateLineNumbers_differentDateIsNotADuplicate() {
        val groceries = category(1, "Groceries")
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(row(2, date = jan5.plusDays(1))),
            existing = listOf(transaction(1, groceries)),
        )

        assertTrue(lines.isEmpty())
    }

    @Test
    fun duplicateLineNumbers_differentCategoryIsNotADuplicate() {
        val groceries = category(1, "Groceries")
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(row(2, categoryName = "Coffee")),
            existing = listOf(transaction(1, groceries)),
        )

        assertTrue(lines.isEmpty())
    }

    // Re-importing an export that overlaps a previous one duplicates rows within the file itself,
    // not just against the database. The second and later copies are the duplicates; the first is not.
    @Test
    fun duplicateLineNumbers_flagsLaterCopiesWithinTheFile() {
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(row(2), row(3), row(4)),
            existing = emptyList(),
        )

        assertEquals(setOf(3, 4), lines)
    }

    @Test
    fun duplicateLineNumbers_ignoresInvalidRows() {
        val lines = ImportPlanner.duplicateLineNumbers(
            rows = listOf(ParsedRow.Invalid(2, RowError.ZERO_AMOUNT, listOf("2026-01-05", "0"))),
            existing = emptyList(),
        )

        assertTrue(lines.isEmpty())
    }

    @Test
    fun duplicateLineNumbers_noExistingTransactions_flagsNothing() {
        val lines = ImportPlanner.duplicateLineNumbers(listOf(row(2)), emptyList())

        assertTrue(lines.isEmpty())
    }

    // Amounts are doubles; keying on raw double text would make 12.5 and 12.50 different rows.
    @Test
    fun duplicateKey_normalisesAmountScale() {
        assertEquals(
            ImportPlanner.duplicateKey(jan5, 12.5, "Groceries", "Food Basics"),
            ImportPlanner.duplicateKey(jan5, 12.50, "Groceries", "Food Basics"),
        )
    }

    @Test
    fun duplicateKey_isCaseInsensitiveOnNames() {
        assertEquals(
            ImportPlanner.duplicateKey(jan5, 12.50, "groceries", "food basics"),
            ImportPlanner.duplicateKey(jan5, 12.50, "Groceries", "Food Basics"),
        )
    }

    @Test
    fun duplicateKey_usesRootLocaleForAmountFormatting() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val key = ImportPlanner.duplicateKey(jan5, 12.50, "Groceries", "Food Basics")
            assertTrue("Key should contain dot-separated amount (12.50), not comma", key.contains("12.50"))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
