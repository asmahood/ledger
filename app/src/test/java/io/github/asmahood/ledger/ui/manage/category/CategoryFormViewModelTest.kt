package io.github.asmahood.ledger.ui.manage.category

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.repository.DuplicateCategoryException
import io.github.asmahood.ledger.data.repository.FakeCategoryRepository
import io.github.asmahood.ledger.rule.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryFormViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeCategoryRepository()

    // Add-mode ViewModel (no categoryId in SavedStateHandle).
    private val viewModel = CategoryFormViewModel(repository, SavedStateHandle())

    // Creates an edit-mode ViewModel pre-loaded with an existing category.
    private fun editViewModel(category: Category): CategoryFormViewModel {
        repository.setCategories(listOf(category))
        return CategoryFormViewModel(
            repository,
            SavedStateHandle(mapOf("categoryId" to category.id))
        )
    }

    // ── Add mode ────────────────────────────────────────────────────────────────

    @Test
    fun formViewModel_initialState_emptyAndInvalid() {
        val state = viewModel.uiState.value

        assertEquals("", state.name)
        assertEquals("", state.description)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertFalse(state.isFormValid)
    }

    @Test
    fun formViewModel_updateName_nonBlank_becomesValid() {
        viewModel.updateName("Groceries")

        val state = viewModel.uiState.value
        assertEquals("Groceries", state.name)
        assertTrue(state.isFormValid)
    }

    @Test
    fun formViewModel_updateName_whitespace_staysInvalid() {
        viewModel.updateName("   ")

        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun formViewModel_updateDescription_doesNotAffectValidity() {
        viewModel.updateDescription("some notes")

        // Name is still blank, so the form is not valid regardless of description.
        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun formViewModel_updateType_updatesType() {
        viewModel.updateType(TransactionType.INCOME)

        assertEquals(TransactionType.INCOME, viewModel.uiState.value.type)
    }

    @Test
    fun formViewModel_saveWhenInvalid_doesNotInsertOrEmit() = runTest {
        viewModel.events.test {
            viewModel.saveCategory()
            expectNoEvents()
        }

        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun formViewModel_saveWhenValid_emitsSavedEvent() = runTest {
        viewModel.updateName("Groceries")

        viewModel.events.test {
            viewModel.saveCategory()
            assertEquals(CategoryFormEvent.SavedSuccessfully, awaitItem())
        }
    }

    @Test
    fun formViewModel_saveWhenValid_insertsMappedCategory() = runTest {
        viewModel.updateName("Groceries")
        viewModel.updateType(TransactionType.EXPENSE)

        viewModel.events.test {
            viewModel.saveCategory()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val inserted = repository.inserted.single()
        assertEquals(0L, inserted.id)
        assertEquals("Groceries", inserted.name)
        assertEquals(TransactionType.EXPENSE, inserted.type)
        assertNull(inserted.description)
    }

    @Test
    fun formViewModel_saveWhenDuplicateName_emitsShowErrorAndDoesNotReportSaved() = runTest {
        repository.insertError = DuplicateCategoryException("A category named \"Groceries\" already exists")
        viewModel.updateName("Groceries")

        viewModel.events.test {
            viewModel.saveCategory()

            val event = awaitItem()
            assertTrue(event is CategoryFormEvent.ShowError)
            assertEquals(
                "A category named \"Groceries\" already exists",
                (event as CategoryFormEvent.ShowError).message,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Edit mode ───────────────────────────────────────────────────────────────

    @Test
    fun editMode_initialState_prePopulatesFromRepository() {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE, description = "Food")
        val vm = editViewModel(category)

        val state = vm.uiState.value
        assertEquals(5L, state.id)
        assertEquals("Groceries", state.name)
        assertEquals("Food", state.description)
        assertEquals(TransactionType.EXPENSE, state.type)
    }

    @Test
    fun editMode_initialState_nullDescription_mapsToEmptyString() {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE, description = null)
        val vm = editViewModel(category)

        assertEquals("", vm.uiState.value.description)
    }

    @Test
    fun editMode_save_callsUpdateNotInsert() = runTest {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE)
        val vm = editViewModel(category)

        vm.events.test {
            vm.saveCategory()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(repository.updated.isNotEmpty())
        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun editMode_save_updatesWithCorrectId() = runTest {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE)
        val vm = editViewModel(category)

        vm.events.test {
            vm.saveCategory()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(5L, repository.updated.single().id)
    }

    @Test
    fun editMode_save_emitsSavedSuccessfully() = runTest {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE)
        val vm = editViewModel(category)

        vm.events.test {
            vm.saveCategory()
            assertEquals(CategoryFormEvent.SavedSuccessfully, awaitItem())
        }
    }

    @Test
    fun editMode_save_duplicateName_emitsShowError() = runTest {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE)
        repository.updateError = DuplicateCategoryException("A category named \"Groceries\" already exists")
        val vm = editViewModel(category)

        vm.events.test {
            vm.saveCategory()

            val event = awaitItem()
            assertTrue(event is CategoryFormEvent.ShowError)
            assertEquals(
                "A category named \"Groceries\" already exists",
                (event as CategoryFormEvent.ShowError).message,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editMode_categoryNotFound_emitsSavedSuccessfully() = runTest {
        // Category absent from repo — simulates deletion between navigation and ViewModel creation.
        val vm = CategoryFormViewModel(
            repository,
            SavedStateHandle(mapOf("categoryId" to 99L))
        )

        vm.events.test {
            assertEquals(CategoryFormEvent.SavedSuccessfully, awaitItem())
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @Test
    fun deleteCategory_emitsSavedSuccessfully() = runTest {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE)
        val vm = editViewModel(category)

        vm.events.test {
            vm.deleteCategory()
            assertEquals(CategoryFormEvent.SavedSuccessfully, awaitItem())
        }
    }

    @Test
    fun deleteCategory_exception_emitsShowError() = runTest {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE)
        repository.deleteError = RuntimeException("FK constraint")
        val vm = editViewModel(category)

        vm.events.test {
            vm.deleteCategory()
            assertTrue(awaitItem() is CategoryFormEvent.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteCategory_callsRepositoryDeleteWithCorrectId() = runTest {
        val category = Category(id = 5L, name = "Groceries", type = TransactionType.EXPENSE)
        val vm = editViewModel(category)

        vm.events.test {
            vm.deleteCategory()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, repository.deleted.size)
        assertEquals(5L, repository.deleted.single().id)
    }
}
