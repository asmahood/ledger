package io.github.asmahood.ledger.ui.transaction.csvimport

import io.github.asmahood.ledger.data.csv.CategoryResolution
import io.github.asmahood.ledger.data.csv.CsvFile
import io.github.asmahood.ledger.data.csv.CsvTable
import io.github.asmahood.ledger.data.csv.FakeCsvFileReader
import io.github.asmahood.ledger.data.csv.ImportField
import io.github.asmahood.ledger.data.csv.ParsedRow
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.FakeCategoryRepository
import io.github.asmahood.ledger.data.repository.FakeTransactionRepository
import io.github.asmahood.ledger.rule.TestDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ImportViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val csvFileReader = FakeCsvFileReader()
    private val transactionRepository = FakeTransactionRepository()
    private val categoryRepository = FakeCategoryRepository()

    private fun viewModel() =
        ImportViewModel(csvFileReader, transactionRepository, categoryRepository)

    private fun csv(vararg lines: String) =
        CsvFile("budget.csv", CsvTable.parse(lines.joinToString("\n")))

    private val standardCsv = csv(
        "Date,Amount,Category,Vendor,Type,Note",
        "2026-01-05,12.50,Groceries,Food Basics,Expense,",
        "2026-01-06,4.25,Coffee,Blue Bottle,Expense,",
    )

    private val groceries = Category(1, "Groceries", TransactionType.EXPENSE)
    private val coffee = Category(2, "Coffee", TransactionType.EXPENSE)

    // --- step 1: file ---

    @Test
    fun initialState_isTheFileStep() = runTest {
        val state = viewModel().uiState.value

        assertEquals(ImportStep.FILE, state.step)
        assertNull(state.fileName)
        assertFalse(state.canGoNext)
    }

    @Test
    fun onFileLoaded_recordsNameHeadersAndAutoDetectedMapping() = runTest {
        val vm = viewModel()

        vm.onFileLoaded(standardCsv)

        val state = vm.uiState.value
        assertEquals("budget.csv", state.fileName)
        assertEquals(listOf("Date", "Amount", "Category", "Vendor", "Type", "Note"), state.headers)
        assertEquals(0, state.mapping[ImportField.DATE])
        assertEquals(2, state.mapping[ImportField.CATEGORY])
        assertTrue(state.canGoNext)
    }

    @Test
    fun onFileLoaded_fileWithNoRows_cannotAdvance() = runTest {
        val vm = viewModel()

        vm.onFileLoaded(csv("Date,Amount,Category"))

        assertFalse(vm.uiState.value.canGoNext)
    }

    // --- step 2: columns ---

    @Test
    fun next_fromFileStep_goesToColumns() = runTest {
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)

        vm.onNext()

        assertEquals(ImportStep.COLUMNS, vm.uiState.value.step)
    }

    @Test
    fun onColumnMapped_updatesTheMapping() = runTest {
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()

        vm.onColumnMapped(ImportField.NOTE, 3)

        assertEquals(3, vm.uiState.value.mapping[ImportField.NOTE])
    }

    @Test
    fun onColumnMapped_null_clearsTheColumn() = runTest {
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()

        vm.onColumnMapped(ImportField.TYPE, null)

        assertNull(vm.uiState.value.mapping[ImportField.TYPE])
    }

    @Test
    fun columnsStep_cannotAdvanceUntilRequiredFieldsAreMapped() = runTest {
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()

        vm.onColumnMapped(ImportField.AMOUNT, null)
        assertFalse(vm.uiState.value.canGoNext)

        vm.onColumnMapped(ImportField.AMOUNT, 1)
        assertTrue(vm.uiState.value.canGoNext)
    }

    // --- step 3: categories ---

    @Test
    fun next_fromColumns_parsesRowsAndListsUnmatchedCategories() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()

        vm.onNext()

        val state = vm.uiState.value
        assertEquals(ImportStep.CATEGORIES, state.step)
        assertEquals(2, state.rows.size)
        assertEquals(listOf("Coffee"), state.unmatchedCategories)
    }

    // With nothing to resolve the step has no content, so showing it would be a dead click.
    @Test
    fun next_fromColumns_skipsCategoriesWhenEveryNameMatches() = runTest {
        categoryRepository.setCategories(listOf(groceries, coffee))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()

        vm.onNext()

        assertEquals(ImportStep.PREVIEW, vm.uiState.value.step)
    }

    @Test
    fun categoriesStep_defaultsEachNameToCreateNewWithTheRowsType() = runTest {
        val vm = viewModel()
        vm.onFileLoaded(
            csv(
                "Date,Amount,Category,Vendor,Type,Note",
                "2026-01-05,1500.00,Salary,Employer,Income,",
            )
        )
        vm.onNext()
        vm.onNext()

        assertEquals(
            CategoryResolution.CreateNew(TransactionType.INCOME),
            vm.uiState.value.resolutions["Salary"],
        )
    }

    @Test
    fun onCategoryResolved_replacesTheResolution() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()

        vm.onCategoryResolved("Coffee", CategoryResolution.UseExisting(groceries.id))

        assertEquals(
            CategoryResolution.UseExisting(groceries.id),
            vm.uiState.value.resolutions["Coffee"],
        )
    }

    // --- step 4: preview ---

    @Test
    fun previewStep_selectsValidRowsAndDeselectsInvalidOnes() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(
            csv(
                "Date,Amount,Category,Vendor,Type,Note",
                "2026-01-05,12.50,Groceries,Food Basics,Expense,",
                "nope,4.25,Groceries,Blue Bottle,Expense,",
            )
        )
        vm.onNext()
        vm.onNext()

        val state = vm.uiState.value
        assertEquals(ImportStep.PREVIEW, state.step)
        assertEquals(setOf(2), state.selectedLines)
        assertTrue(state.rows[1] is ParsedRow.Invalid)
    }

    @Test
    // Re-importing a file whose category was mapped onto an existing category with a different
    // spelling: the rows were stored under the app's name, so detection must compare against that,
    // not against the name still spelled in the CSV.
    fun previewStep_flagsDuplicatesOfRowsStoredUnderARemappedCategory() = runTest {
        val restaurants = Category(3, "Restaurants", TransactionType.EXPENSE)
        categoryRepository.setCategories(listOf(restaurants))
        transactionRepository.setTransactions(
            listOf(
                Transaction(
                    1, 13.99, LocalDate.of(2026, 1, 6), "McDonalds",
                    TransactionType.EXPENSE, null, restaurants,
                )
            )
        )
        val vm = viewModel()
        vm.onFileLoaded(
            csv(
                "Date,Amount,Category,Vendor,Type,Note",
                "2026-01-06,13.99,Restaurant,McDonalds,Expense,",
            )
        )
        vm.onNext()
        vm.onNext()

        vm.onCategoryResolved("Restaurant", CategoryResolution.UseExisting(restaurants.id))
        vm.onNext()

        assertEquals(ImportStep.PREVIEW, vm.uiState.value.step)
        assertEquals(setOf(2), vm.uiState.value.duplicateLines)
    }

    @Test
    fun previewStep_flagsDuplicatesButLeavesThemSelected() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        transactionRepository.setTransactions(
            listOf(
                Transaction(
                    1, 12.50, LocalDate.of(2026, 1, 5), "Food Basics",
                    TransactionType.EXPENSE, null, groceries,
                )
            )
        )
        val vm = viewModel()
        vm.onFileLoaded(
            csv(
                "Date,Amount,Category,Vendor,Type,Note",
                "2026-01-05,12.50,Groceries,Food Basics,Expense,",
            )
        )
        vm.onNext()
        vm.onNext()

        val state = vm.uiState.value
        assertEquals(setOf(2), state.duplicateLines)
        assertEquals(setOf(2), state.selectedLines)
    }

    @Test
    fun onRowToggled_addsAndRemovesASelection() = runTest {
        categoryRepository.setCategories(listOf(groceries, coffee))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()

        vm.onRowToggled(2)
        assertEquals(setOf(3), vm.uiState.value.selectedLines)

        vm.onRowToggled(2)
        assertEquals(setOf(2, 3), vm.uiState.value.selectedLines)
    }

    // --- import ---

    @Test
    fun onImport_writesSelectedRowsAsOneBatch() = runTest {
        categoryRepository.setCategories(listOf(groceries, coffee))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()

        vm.onImport()

        assertEquals(1, transactionRepository.insertedBatches.size)
        val batch = transactionRepository.insertedBatches.single()
        assertEquals(2, batch.size)
        assertEquals(setOf("Food Basics", "Blue Bottle"), batch.map { it.vendor }.toSet())
        assertEquals(groceries, batch.first { it.vendor == "Food Basics" }.category)
    }

    @Test
    fun onImport_skipsDeselectedRows() = runTest {
        categoryRepository.setCategories(listOf(groceries, coffee))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()
        vm.onRowToggled(3)

        vm.onImport()

        assertEquals(1, transactionRepository.insertedBatches.single().size)
    }

    @Test
    fun onImport_createsCategoriesResolvedAsNew() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()
        vm.onNext()

        vm.onImport()

        assertEquals(listOf("Coffee"), categoryRepository.inserted.map { it.name })
        assertEquals(TransactionType.EXPENSE, categoryRepository.inserted.single().type)
        assertNull(categoryRepository.inserted.single().budget)
    }

    @Test
    fun onImport_dropsRowsWhoseCategoryWasSkipped() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()
        vm.onCategoryResolved("Coffee", CategoryResolution.SkipRows)
        vm.onNext()

        vm.onImport()

        val batch = transactionRepository.insertedBatches.single()
        assertEquals(listOf("Food Basics"), batch.map { it.vendor })
        assertEquals(1, vm.uiState.value.result?.categorySkipped)
    }

    @Test
    fun onImport_reportsASummaryAndEndsOnTheResultStep() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        transactionRepository.setTransactions(
            listOf(
                Transaction(
                    1, 12.50, LocalDate.of(2026, 1, 5), "Food Basics",
                    TransactionType.EXPENSE, null, groceries,
                )
            )
        )
        val vm = viewModel()
        vm.onFileLoaded(
            csv(
                "Date,Amount,Category,Vendor,Type,Note",
                "2026-01-05,12.50,Groceries,Food Basics,Expense,",
                "2026-01-06,4.25,Coffee,Blue Bottle,Expense,",
                "nope,9.99,Groceries,Somewhere,Expense,",
            )
        )
        vm.onNext()
        vm.onNext()
        vm.onNext()
        vm.onRowToggled(2)

        vm.onImport()

        val result = vm.uiState.value.result
        assertEquals(ImportStep.RESULT, vm.uiState.value.step)
        assertNotNull(result)
        assertEquals(1, result!!.imported)
        assertEquals(1, result.invalid)
        assertEquals(1, result.duplicatesSkipped)
        assertEquals(0, result.categorySkipped)
        assertEquals(1, result.categoriesCreated)
    }

    // Regression: a name only ever lands in `resolutions` when it matched no existing category,
    // so choosing "use existing" for it used to guarantee a lookup miss that crashed the whole
    // import (see finding 1).
    @Test
    fun onImport_useExisting_mapsRowsToTheChosenCategory() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()
        vm.onCategoryResolved("Coffee", CategoryResolution.UseExisting(groceries.id))
        vm.onNext()

        vm.onImport()

        val batch = transactionRepository.insertedBatches.single()
        assertEquals(2, batch.size)
        assertTrue(batch.all { it.category == groceries })
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(0, categoryRepository.inserted.size)
    }

    // Regression: categorySkipped used to require the row to still be selected, so a valid,
    // non-duplicate row whose category was skipped landed in no bucket at all once deselected
    // (see finding 2).
    @Test
    fun onImport_categorySkipped_countsDeselectedNonDuplicateRowsToo() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()
        vm.onCategoryResolved("Coffee", CategoryResolution.SkipRows)
        vm.onNext()
        vm.onRowToggled(3)

        vm.onImport()

        val result = vm.uiState.value.result
        assertEquals(1, result?.imported)
        assertEquals(1, result?.categorySkipped)
    }

    // Regression: resolutions used to be keyed by the exact first-seen spelling, so a row
    // spelling an already-resolved name with different case missed the map entirely (see
    // finding 4). Both spellings should be skipped once "Coffee" is resolved to SkipRows.
    @Test
    fun onImport_resolutionLookup_isCaseInsensitive() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(
            csv(
                "Date,Amount,Category,Vendor,Type,Note",
                "2026-01-05,12.50,Groceries,Food Basics,Expense,",
                "2026-01-06,4.25,Coffee,Blue Bottle,Expense,",
                "2026-01-07,3.00,coffee,Second Cup,Expense,",
            )
        )
        vm.onNext()
        vm.onNext()
        vm.onCategoryResolved("Coffee", CategoryResolution.SkipRows)
        vm.onNext()

        vm.onImport()

        val batch = transactionRepository.insertedBatches.single()
        assertEquals(listOf("Food Basics"), batch.map { it.vendor })
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(2, vm.uiState.value.result?.categorySkipped)
    }

    // Regression: onImport captured state once and finalized with values derived from that
    // stale snapshot; a row toggle landing while category creation was suspended used to be
    // silently ignored by the actual import (see finding 3).
    @Test
    fun onImport_rowToggledWhileCategoryCreationIsInFlight_isHonoured() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()
        vm.onNext()

        val resumeCategoryCreation = CompletableDeferred<Unit>()
        categoryRepository.onInsertCategory = { resumeCategoryCreation.await() }

        vm.onImport()
        // The coroutine above ran eagerly (UnconfinedTestDispatcher) up to the suspend point
        // inside insertCategory("Coffee") and is now parked there.
        vm.onRowToggled(3)
        resumeCategoryCreation.complete(Unit)

        val batch = transactionRepository.insertedBatches.single()
        assertEquals(listOf("Food Basics"), batch.map { it.vendor })
        assertEquals(1, vm.uiState.value.result?.imported)
    }

    @Test
    fun onImport_writeFailure_surfacesAnError() = runTest {
        categoryRepository.setCategories(listOf(groceries, coffee))
        transactionRepository.insertError = RuntimeException("disk full")
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()

        vm.onImport()

        val state = vm.uiState.value
        assertEquals(ImportStep.PREVIEW, state.step)
        assertEquals("disk full", state.errorMessage)
        assertNull(state.result)
    }

    // --- navigation ---

    @Test
    fun back_fromColumns_returnsToFile() = runTest {
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()

        vm.onBack()

        assertEquals(ImportStep.FILE, vm.uiState.value.step)
    }

    @Test
    fun back_fromPreview_returnsToCategoriesWhenThereWereAny() = runTest {
        categoryRepository.setCategories(listOf(groceries))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()
        vm.onNext()

        vm.onBack()

        assertEquals(ImportStep.CATEGORIES, vm.uiState.value.step)
    }

    @Test
    fun back_fromPreview_skipsCategoriesWhenThereWereNone() = runTest {
        categoryRepository.setCategories(listOf(groceries, coffee))
        val vm = viewModel()
        vm.onFileLoaded(standardCsv)
        vm.onNext()
        vm.onNext()

        vm.onBack()

        assertEquals(ImportStep.COLUMNS, vm.uiState.value.step)
    }
}
