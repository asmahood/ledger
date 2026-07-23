package io.github.asmahood.ledger.ui.transaction.csvimport

import io.github.asmahood.ledger.data.csv.CategoryResolution
import io.github.asmahood.ledger.data.csv.ColumnMapping
import io.github.asmahood.ledger.data.csv.ImportPlanner
import io.github.asmahood.ledger.data.csv.ParsedRow
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction

enum class ImportStep { FILE, COLUMNS, CATEGORIES, PREVIEW, RESULT }

data class ImportResult(
    val imported: Int,
    val invalid: Int,
    val duplicatesSkipped: Int,
    val categorySkipped: Int,
    val categoriesCreated: Int,
)

data class ImportUiState(
    val step: ImportStep = ImportStep.FILE,
    val fileName: String? = null,
    val headers: List<String> = emptyList(),
    val mapping: ColumnMapping = ColumnMapping.EMPTY,
    val rawRows: List<List<String>> = emptyList(),
    val rows: List<ParsedRow> = emptyList(),
    val duplicateLines: Set<Int> = emptySet(),
    val unmatchedCategories: List<String> = emptyList(),
    val resolutions: Map<String, CategoryResolution> = emptyMap(),
    val existingCategories: List<Category> = emptyList(),
    /** Snapshot taken when the plan was built; the basis for duplicate detection. */
    val existingTransactions: List<Transaction> = emptyList(),
    val selectedLines: Set<Int> = emptySet(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val result: ImportResult? = null,
) {
    val canGoNext: Boolean
        get() = when (step) {
            ImportStep.FILE -> fileName != null && rawRows.isNotEmpty()
            ImportStep.COLUMNS -> mapping.isComplete
            ImportStep.CATEGORIES -> true
            ImportStep.PREVIEW, ImportStep.RESULT -> false
        }

    val validRowCount: Int
        get() = rows.count { it is ParsedRow.Valid }

    /**
     * The category name a row will actually be stored under. A row resolved to an existing
     * category is stored under *that* category's name, which is often spelled differently from
     * the CSV's — "Restaurant" in the file mapped onto "Restaurants" in the app.
     */
    fun storedCategoryNameFor(row: ParsedRow.Valid): String {
        val resolution = resolutions.entries
            .find { it.key.equals(row.categoryName, ignoreCase = true) }
            ?.value
        return when (resolution) {
            is CategoryResolution.UseExisting ->
                existingCategories.find { it.id == resolution.categoryId }?.name
                    ?: row.categoryName

            else -> row.categoryName
        }
    }

    /** Duplicate lines recomputed against the categories rows will actually be stored under. */
    fun recomputeDuplicateLines(): Set<Int> = ImportPlanner.duplicateLineNumbers(
        rows = rows,
        existing = existingTransactions,
        categoryNameFor = ::storedCategoryNameFor,
    )

    val selectedCount: Int
        get() = selectedLines.size
}
