package io.github.asmahood.ledger.ui.transaction.form

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
class AddTransactionFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * Creates a category through the Add Category form so it is available in the transaction
     * form's category dropdown. Defaults to Expense; pass [income] = true to tap the Income
     * segmented button first. Returns once the category is listed in Manage.
     */
    private fun createCategory(name: String, income: Boolean = false) {
        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithContentDescription("Add Category").performClick()
        if (income) {
            composeRule.onNodeWithText("Income").performClick()
        }
        composeRule.onNodeWithText("Name").performTextInput(name)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the Add Transaction form via the FAB on the Transactions tab. */
    private fun openTransactionForm() {
        composeRule.onNodeWithText("Transactions").performClick()
        composeRule.onNodeWithContentDescription("Add a new transaction").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Amount").fetchSemanticsNodes().isNotEmpty()
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

    @Test
    fun addExpense_fromFab_savesAndReturns() {
        val categoryName = "UITestExpense${System.currentTimeMillis()}"
        createCategory(categoryName)

        openTransactionForm()

        composeRule.onNodeWithText("Amount").performTextInput("42.00")
        pickCategory(categoryName)
        // Expense vendor field is labelled "Store/Vendor".
        composeRule.onNodeWithText("Store/Vendor").performTextInput("Food Basics")

        composeRule.onNodeWithText("Save").performClick()

        // A successful save navigates back; the form (and its FAB) gives way to the
        // Transactions list, where the FAB is shown again.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Add transaction").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithContentDescription("Add a new transaction").assertIsDisplayed()
    }

    @Test
    fun addIncome_fromFab_savesAndReturns() {
        val categoryName = "UITestIncome${System.currentTimeMillis()}"
        createCategory(categoryName, income = true)

        openTransactionForm()

        // Switch the form to Income so income categories appear and the vendor label changes.
        composeRule.onNodeWithText("Income").performClick()

        composeRule.onNodeWithText("Amount").performTextInput("1500.00")
        pickCategory(categoryName)
        // Income vendor field is labelled "Source/Payer".
        composeRule.onNodeWithText("Source/Payer").performTextInput("Employer")

        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Add transaction").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithContentDescription("Add a new transaction").assertIsDisplayed()
    }
}
