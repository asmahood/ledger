package io.github.asmahood.ledger.ui.manage.category

import app.cash.turbine.test
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
    private val viewModel = CategoryFormViewModel(repository)

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
}
