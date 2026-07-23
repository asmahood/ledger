package io.github.asmahood.ledger.data.csv

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import java.time.LocalDate
import java.util.Locale

sealed interface CategoryResolution {
    data class UseExisting(val categoryId: Long) : CategoryResolution
    data class CreateNew(val type: TransactionType) : CategoryResolution
    data object SkipRows : CategoryResolution
}

object ImportPlanner {
    fun unmatchedCategoryNames(rows: List<ParsedRow>, existing: List<Category>): List<String> {
        val validRows = rows.filterIsInstance<ParsedRow.Valid>()
        val existingLowercased = existing.map { it.name.lowercase() }.toSet()

        return validRows
            .map { it.categoryName }
            .filter { it.lowercase() !in existingLowercased }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    fun defaultTypeFor(name: String, rows: List<ParsedRow>): TransactionType {
        val validRows = rows.filterIsInstance<ParsedRow.Valid>()
        val matchingRows = validRows.filter { it.categoryName.lowercase() == name.lowercase() }

        if (matchingRows.isEmpty()) {
            return TransactionType.EXPENSE
        }

        val types = matchingRows.map { it.type }.toSet()
        return if (types.size == 1) types.first() else TransactionType.EXPENSE
    }

    /**
     * Line numbers of rows that duplicate an existing transaction or an earlier row in the file.
     *
     * [categoryNameFor] returns the name a row's category will actually be *stored* under, which
     * is not always the name spelled in the CSV: a row whose category was resolved to an existing
     * category is stored under that category's name. Keying on the CSV's spelling instead would
     * make every remapped row invisible to duplicate detection.
     */
    fun duplicateLineNumbers(
        rows: List<ParsedRow>,
        existing: List<Transaction>,
        categoryNameFor: (ParsedRow.Valid) -> String = { it.categoryName },
    ): Set<Int> {
        val validRows = rows.filterIsInstance<ParsedRow.Valid>()
        val seenKeys = existing.map { duplicateKey(it.date, it.amount, it.category.name, it.vendor) }.toMutableSet()
        val duplicates = mutableSetOf<Int>()

        for (row in validRows) {
            val key = duplicateKey(row.date, row.amount, categoryNameFor(row), row.vendor)
            if (key in seenKeys) {
                duplicates.add(row.lineNumber)
            }
            seenKeys.add(key)
        }

        return duplicates
    }

    fun duplicateKey(date: LocalDate, amount: Double, categoryName: String, vendor: String): String {
        return "$date|${"%.2f".format(Locale.ROOT, amount)}|${vendor.lowercase()}|${categoryName.lowercase()}"
    }
}
