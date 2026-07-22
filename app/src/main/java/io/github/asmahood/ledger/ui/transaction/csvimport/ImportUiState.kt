package io.github.asmahood.ledger.ui.transaction.csvimport

import io.github.asmahood.ledger.data.csv.CategoryResolution
import io.github.asmahood.ledger.data.csv.ColumnMapping
import io.github.asmahood.ledger.data.csv.ParsedRow
import io.github.asmahood.ledger.data.model.Category

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

    val selectedCount: Int
        get() = selectedLines.size
}
