package io.github.asmahood.ledger.ui.transaction.csvimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.asmahood.ledger.data.csv.CategoryResolution
import io.github.asmahood.ledger.data.csv.ColumnMapping
import io.github.asmahood.ledger.data.csv.CsvFile
import io.github.asmahood.ledger.data.csv.CsvFileReader
import io.github.asmahood.ledger.data.csv.ImportField
import io.github.asmahood.ledger.data.csv.ImportPlanner
import io.github.asmahood.ledger.data.csv.ParsedRow
import io.github.asmahood.ledger.data.csv.RowParser
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.repository.CategoryRepository
import io.github.asmahood.ledger.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val csvFileReader: CsvFileReader,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { csvFileReader.read(uri) }
                .onSuccess { file -> onFileLoaded(file) }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun onFileLoaded(file: CsvFile) {
        _uiState.value = ImportUiState(
            fileName = file.name,
            headers = file.table.headers,
            rawRows = file.table.rows,
            mapping = ColumnMapping.autoDetect(file.table.headers),
        )
    }

    fun onColumnMapped(field: ImportField, columnIndex: Int?) {
        _uiState.update { it.copy(mapping = it.mapping.with(field, columnIndex)) }
    }

    fun onCategoryResolved(name: String, resolution: CategoryResolution) {
        _uiState.update { it.copy(resolutions = it.resolutions + (name to resolution)) }
    }

    fun onRowToggled(lineNumber: Int) {
        _uiState.update {
            val selected = it.selectedLines
            it.copy(
                selectedLines = if (lineNumber in selected) selected - lineNumber else selected + lineNumber,
            )
        }
    }

    fun onNext() {
        when (_uiState.value.step) {
            ImportStep.FILE -> _uiState.update { it.copy(step = ImportStep.COLUMNS) }
            ImportStep.COLUMNS -> parseAndPlan()
            // Resolutions chosen on the categories step change which category name a row is
            // stored under, so the duplicate set has to be rebuilt before the preview shows it.
            ImportStep.CATEGORIES -> _uiState.update {
                it.copy(step = ImportStep.PREVIEW, duplicateLines = it.recomputeDuplicateLines())
            }
            ImportStep.PREVIEW, ImportStep.RESULT -> Unit
        }
    }

    fun onBack() {
        when (_uiState.value.step) {
            ImportStep.FILE, ImportStep.RESULT -> Unit
            ImportStep.COLUMNS -> _uiState.update { it.copy(step = ImportStep.FILE) }
            ImportStep.CATEGORIES -> _uiState.update { it.copy(step = ImportStep.COLUMNS) }
            ImportStep.PREVIEW -> _uiState.update {
                val previousStep = if (it.unmatchedCategories.isNotEmpty()) {
                    ImportStep.CATEGORIES
                } else {
                    ImportStep.COLUMNS
                }
                it.copy(step = previousStep)
            }
        }
    }

    private fun parseAndPlan() {
        viewModelScope.launch {
            val state = _uiState.value
            val rows = state.rawRows.mapIndexed { index, row ->
                RowParser.parse(row, state.mapping, lineNumber = index + 2)
            }

            val existingCategories = categoryRepository.getAllCategoriesStream().first()
            val existingTransactions = transactionRepository.getAllTransactionsStream().first()

            val unmatchedCategories = ImportPlanner.unmatchedCategoryNames(rows, existingCategories)
            val duplicateLines = ImportPlanner.duplicateLineNumbers(rows, existingTransactions)
            val defaultResolutions = unmatchedCategories.associateWith { name ->
                CategoryResolution.CreateNew(ImportPlanner.defaultTypeFor(name, rows))
            }
            val defaultSelectedLines = rows.filterIsInstance<ParsedRow.Valid>()
                .map { it.lineNumber }
                .toSet()

            val nextStep = if (unmatchedCategories.isNotEmpty()) ImportStep.CATEGORIES else ImportStep.PREVIEW

            _uiState.update { current ->
                // These results were computed from the file that was loaded when this parse
                // started. If a different file has since been loaded (which fully replaces the
                // state), that load already superseded this one - applying our stale results on
                // top of it would mix rows from one file with headers/mapping from another.
                if (current.fileName != state.fileName || current.rawRows != state.rawRows) {
                    current
                } else {
                    current.copy(
                        step = nextStep,
                        rows = rows,
                        existingCategories = existingCategories,
                        existingTransactions = existingTransactions,
                        unmatchedCategories = unmatchedCategories,
                        duplicateLines = duplicateLines,
                        // A concurrent onCategoryResolved for one of these names wins over the
                        // freshly computed default.
                        resolutions = defaultResolutions + current.resolutions,
                        selectedLines = defaultSelectedLines,
                    )
                }
            }
        }
    }

    fun onImport() {
        viewModelScope.launch {
            val state = _uiState.value

            runCatching {
                val createdCategories = mutableListOf<Category>()
                for ((name, resolution) in state.resolutions) {
                    if (resolution is CategoryResolution.CreateNew) {
                        val id = categoryRepository.insertCategory(
                            Category(id = 0, name = name, type = resolution.type),
                        )
                        createdCategories += Category(id = id, name = name, type = resolution.type)
                    }
                }

                // Creating categories above suspends. Re-read the state now that it's done, so a
                // row toggle or resolution change made while we were waiting isn't lost.
                val current = _uiState.value
                val existingById = current.existingCategories.associateBy { it.id }
                val resolutionsByName = current.resolutions.mapKeys { ImportPlanner.normalizeName(it.key) }

                val lookup = mutableMapOf<String, Category>()
                current.existingCategories.forEach { lookup[ImportPlanner.normalizeName(it.name)] = it }
                resolutionsByName.forEach { (name, resolution) ->
                    if (resolution is CategoryResolution.UseExisting) {
                        existingById[resolution.categoryId]?.let { lookup[name] = it }
                    }
                }
                createdCategories.forEach { lookup[ImportPlanner.normalizeName(it.name)] = it }

                fun resolutionFor(categoryName: String) = resolutionsByName[ImportPlanner.normalizeName(categoryName)]

                val validRows = current.rows.filterIsInstance<ParsedRow.Valid>()
                val duplicateSkippedLines = current.duplicateLines - current.selectedLines

                val transactions = validRows
                    .filter { it.lineNumber in current.selectedLines }
                    .mapNotNull { row ->
                        val category = lookup[ImportPlanner.normalizeName(row.categoryName)]
                        if (category == null || resolutionFor(row.categoryName) is CategoryResolution.SkipRows) {
                            null
                        } else {
                            Transaction(
                                id = 0,
                                amount = row.amount,
                                date = row.date,
                                vendor = row.vendor,
                                type = row.type,
                                notes = row.note,
                                category = category,
                            )
                        }
                    }

                transactionRepository.insertTransactions(transactions)

                val invalid = current.rows.count { it is ParsedRow.Invalid }
                val duplicatesSkipped = duplicateSkippedLines.size
                val categorySkipped = validRows.count { row ->
                    row.lineNumber !in duplicateSkippedLines &&
                        (
                            resolutionFor(row.categoryName) is CategoryResolution.SkipRows ||
                                lookup[row.categoryName.lowercase()] == null
                            )
                }

                ImportResult(
                    imported = transactions.size,
                    invalid = invalid,
                    duplicatesSkipped = duplicatesSkipped,
                    categorySkipped = categorySkipped,
                    categoriesCreated = createdCategories.size,
                )
            }.onSuccess { result ->
                _uiState.update { it.copy(step = ImportStep.RESULT, result = result, errorMessage = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }
}
