package io.github.asmahood.ledger.ui.transaction.form

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class EditTransactionFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /** Creates an expense category so it is available in the transaction form's dropdown. */
    private fun createCategory(name: String) {
        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithContentDescription("Add Category").performClick()
        composeRule.onNodeWithText("Name").performTextInput(name)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Selects [categoryName] from the readonly category dropdown. */
    private fun pickCategory(categoryName: String) {
        composeRule.onNodeWithText("Pick a category").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(categoryName).performClick()
    }

    /**
     * Creates an expense transaction with the given [vendor] via the Add Transaction form and
     * waits until the card appears in the Transactions list.
     */
    private fun createTransaction(vendor: String, amount: String = "42.00") {
        val categoryName = "EditTxnCat${System.currentTimeMillis()}"
        createCategory(categoryName)

        composeRule.onNodeWithText("Transactions").performClick()
        composeRule.onNodeWithContentDescription("Add a new transaction").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Amount").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Amount").performTextInput(amount)
        pickCategory(categoryName)
        composeRule.onNodeWithText("Store/Vendor").performTextInput(vendor)
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(vendor).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun editTransaction_fromList_updatesVendorInList() {
        val originalVendor = "Original Vendor"
        val updatedVendor = "Updated Vendor"

        createTransaction(originalVendor)

        // Tap the transaction card to open the Edit screen.
        composeRule.onNodeWithText(originalVendor).performClick()

        // Wait for the ViewModel to populate the form (vendor field pre-filled).
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Edit Transaction").fetchSemanticsNodes().isNotEmpty()
        }

        // Replace the vendor and save.
        composeRule.onNodeWithText(originalVendor).performTextReplacement(updatedVendor)
        composeRule.onNodeWithText("Save").performClick()

        // Saving navigates back to the list; the updated vendor should appear.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(updatedVendor).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(updatedVendor).assertIsDisplayed()
    }

    @Test
    fun deleteTransaction_confirmingDialog_removesFromList() {
        val vendor = "Vendor To Delete"

        createTransaction(vendor)

        // Open the Edit screen.
        composeRule.onNodeWithText(vendor).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Edit Transaction").fetchSemanticsNodes().isNotEmpty()
        }

        // Open the delete confirmation dialog via the toolbar icon.
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()

        // Confirm deletion — the dialog's "Delete" TextButton is distinct from the icon
        // button above (which uses contentDescription, not visible text).
        composeRule.onNodeWithText("Delete").performClick()

        // Confirming deletes the transaction and navigates back; wait until it disappears.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(vendor).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun deleteTransaction_cancellingDialog_remainsOnEditScreen() {
        val vendor = "Vendor To Keep"

        createTransaction(vendor)

        // Open the Edit screen.
        composeRule.onNodeWithText(vendor).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Edit Transaction").fetchSemanticsNodes().isNotEmpty()
        }

        // Open the delete confirmation dialog.
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()

        // Dismiss the dialog by clicking the "Cancel" button inside it.
        // The dialog's Cancel button has "Delete" as a sibling; nothing else does.
        composeRule.onAllNodesWithText("Cancel")
            .filterToOne(hasAnySibling(hasText("Delete")))
            .performClick()

        // Dialog is gone; we should still be on the Edit Transaction screen.
        composeRule.onNodeWithText("Edit Transaction").assertIsDisplayed()
    }
}
