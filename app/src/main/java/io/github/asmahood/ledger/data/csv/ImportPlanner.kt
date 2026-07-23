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
    /**
     * Comparison form for a category or vendor name: case-folded **and trimmed**.
     *
     * Trimming matters as much as case here. A category saved in the app as `"Restaurant "` and a
     * CSV cell reading `"Restaurant"` are the same category to a human, and treating them as
     * different silently breaks both category matching and duplicate detection.
     */
    fun normalizeName(name: String): String = name.trim().lowercase()

    fun unmatchedCategoryNames(rows: List<ParsedRow>, existing: List<Category>): List<String> {
        val validRows = rows.filterIsInstance<ParsedRow.Valid>()
        val existingNormalized = existing.map { normalizeName(it.name) }.toSet()

        return validRows
            .map { it.categoryName }
            .filter { normalizeName(it) !in existingNormalized }
            .distinctBy { normalizeName(it) }
            .sortedBy { normalizeName(it) }
    }

    fun defaultTypeFor(name: String, rows: List<ParsedRow>): TransactionType {
        val validRows = rows.filterIsInstance<ParsedRow.Valid>()
        val matchingRows = validRows.filter { normalizeName(it.categoryName) == normalizeName(name) }

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
        return "$date|${"%.2f".format(Locale.ROOT, amount)}|${normalizeName(vendor)}|${normalizeName(categoryName)}"
    }
}
