package io.github.asmahood.ledger.ui.manage.category

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
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
class AddCategoryFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun addCategory_fromManage_appearsInList() {
        val categoryName = "UI Test Category"

        // App starts on Overview; switch to the Manage tab.
        composeRule.onNodeWithText("Manage").performClick()

        // Tap the + action in the Manage top bar to open the Add Category form.
        composeRule.onNodeWithContentDescription("Add Category").performClick()

        // Fill in the name (type defaults to Expense) and save.
        composeRule.onNodeWithText("Name").performTextInput(categoryName)
        composeRule.onNodeWithText("Save").performClick()

        // Saving navigates back to Manage, where the new category should now be listed.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(categoryName).assertIsDisplayed()
        // A category with no budget shows "-" in the trailing pill.
        composeRule.onNode(hasText(categoryName) and hasText("-")).assertIsDisplayed()
    }

    @Test
    fun addCategory_withBudget_savesAndAppearsInList() {
        val categoryName = "Budgeted Category"

        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithContentDescription("Add Category").performClick()

        // Fill in the name and a monthly budget, then save.
        composeRule.onNodeWithText("Name").performTextInput(categoryName)
        composeRule.onNodeWithText("Budget").performTextInput("250.00")
        composeRule.onNodeWithText("Save").performClick()

        // A valid budget does not block saving; the category lands back in the Manage list.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(categoryName).assertIsDisplayed()
        // The trailing pill shows the formatted amount with a "/mo" suffix.
        composeRule.onNode(hasText(categoryName) and hasText("$250.00/mo")).assertIsDisplayed()
    }
}
