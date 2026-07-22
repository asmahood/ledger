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
            ImportStep.CATEGORIES -> _uiState.update { it.copy(step = ImportStep.PREVIEW) }
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
            val resolutions = unmatchedCategories.associateWith { name ->
                CategoryResolution.CreateNew(ImportPlanner.defaultTypeFor(name, rows))
            }
            val selectedLines = rows.filterIsInstance<ParsedRow.Valid>()
                .map { it.lineNumber }
                .toSet()

            val nextStep = if (unmatchedCategories.isNotEmpty()) ImportStep.CATEGORIES else ImportStep.PREVIEW

            _uiState.update {
                it.copy(
                    step = nextStep,
                    rows = rows,
                    existingCategories = existingCategories,
                    unmatchedCategories = unmatchedCategories,
                    duplicateLines = duplicateLines,
                    resolutions = resolutions,
                    selectedLines = selectedLines,
                )
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

                val lookup = mutableMapOf<String, Category>()
                state.existingCategories.forEach { lookup[it.name.lowercase()] = it }
                createdCategories.forEach { lookup[it.name.lowercase()] = it }

                val validRows = state.rows.filterIsInstance<ParsedRow.Valid>()

                val transactions = validRows
                    .filter { it.lineNumber in state.selectedLines }
                    .filter { state.resolutions[it.categoryName] !is CategoryResolution.SkipRows }
                    .map { row ->
                        Transaction(
                            id = 0,
                            amount = row.amount,
                            date = row.date,
                            vendor = row.vendor,
                            type = row.type,
                            notes = row.note,
                            category = lookup.getValue(row.categoryName.lowercase()),
                        )
                    }

                transactionRepository.insertTransactions(transactions)

                val invalid = state.rows.count { it is ParsedRow.Invalid }
                val duplicatesSkipped = state.duplicateLines.count { it !in state.selectedLines }
                val categorySkipped = validRows.count { row ->
                    row.lineNumber in state.selectedLines &&
                        state.resolutions[row.categoryName] is CategoryResolution.SkipRows
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
