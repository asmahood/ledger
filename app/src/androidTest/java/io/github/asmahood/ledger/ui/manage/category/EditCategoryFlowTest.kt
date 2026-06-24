package io.github.asmahood.ledger.ui.manage.category

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.asmahood.ledger.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EditCategoryFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /** Creates a category via the Add Category form and waits until it appears in the Manage list. */
    private fun createCategory(name: String) {
        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithContentDescription("Add Category").performClick()
        composeRule.onNodeWithText("Name").performTextInput(name)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun editCategory_fromManage_updatesNameInList() {
        val originalName = "Edit Test Category"
        val updatedName = "Edited Category Name"

        createCategory(originalName)

        // Tap the category card to open the Edit screen.
        composeRule.onNodeWithText(originalName).performClick()

        // Wait for the ViewModel to load the category into the text field.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(originalName).fetchSemanticsNodes().isNotEmpty()
        }

        // Replace the name and save.
        composeRule.onNodeWithText(originalName).performTextReplacement(updatedName)
        composeRule.onNodeWithText("Save").performClick()

        // Saving navigates back to Manage; the updated name should appear in the list.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(updatedName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(updatedName).assertIsDisplayed()
    }

    @Test
    fun editCategory_enterBudgetThenClearIt_savesEachTime() {
        val categoryName = "Budget Edit Category"

        createCategory(categoryName)

        // Open the Edit screen and wait for the form to populate.
        composeRule.onNodeWithText(categoryName).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }

        // Enter a monthly budget and save.
        composeRule.onNodeWithText("Budget").performTextInput("300.00")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }

        // Reopen the Edit screen; the budget field is prefilled with the saved amount.
        composeRule.onNodeWithText(categoryName).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("300.00").fetchSemanticsNodes().isNotEmpty()
        }

        // Clear the budget by emptying the field and save again.
        composeRule.onNodeWithText("300.00").performTextClearance()
        composeRule.onNodeWithText("Save").performClick()

        // Clearing is a valid save; we navigate back to Manage with the category intact.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(categoryName).assertIsDisplayed()
    }

    @Test
    fun deleteCategory_confirmingDialog_removesFromList() {
        val categoryName = "Category To Delete"

        createCategory(categoryName)

        // Navigate to the Edit screen.
        composeRule.onNodeWithText(categoryName).performClick()

        // Wait for the ViewModel to populate the form.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }

        // Open the delete confirmation dialog.
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()

        // Confirm deletion — the dialog's "Delete" TextButton is distinct from the icon
        // button above (which uses contentDescription, not visible text).
        composeRule.onNodeWithText("Delete").performClick()

        // Confirming deletes the category and navigates back; wait until it disappears.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun deleteCategory_cancellingDialog_remainsOnEditScreen() {
        val categoryName = "Category To Keep"

        createCategory(categoryName)

        // Navigate to the Edit screen.
        composeRule.onNodeWithText(categoryName).performClick()

        // Wait for the ViewModel to populate the form.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }

        // Open the delete confirmation dialog.
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()

        // Dismiss the dialog by clicking the "Cancel" button inside it.
        // The dialog's Cancel button has "Delete" as a sibling; the form's does not.
        composeRule.onAllNodesWithText("Cancel")
            .filterToOne(hasAnySibling(hasText("Delete")))
            .performClick()

        // Dialog is gone; we should still be on the Edit Category screen.
        composeRule.onNodeWithText("Edit Category").assertIsDisplayed()
    }
}
