package io.github.asmahood.ledger.data.csv

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import java.time.LocalDate

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

    fun duplicateLineNumbers(rows: List<ParsedRow>, existing: List<Transaction>): Set<Int> {
        val validRows = rows.filterIsInstance<ParsedRow.Valid>()
        val seenKeys = existing.map { duplicateKey(it.date, it.amount, it.category.name, it.vendor) }.toMutableSet()
        val duplicates = mutableSetOf<Int>()

        for (row in validRows) {
            val key = duplicateKey(row.date, row.amount, row.categoryName, row.vendor)
            if (key in seenKeys) {
                duplicates.add(row.lineNumber)
            }
            seenKeys.add(key)
        }

        return duplicates
    }

    fun duplicateKey(date: LocalDate, amount: Double, categoryName: String, vendor: String): String {
        return "$date|${"%.2f".format(amount)}|${vendor.lowercase()}|${categoryName.lowercase()}"
    }
}
