package io.github.asmahood.ledger.ui.overview

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverviewScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleSummary = OverviewSummary(
        income = 4200.0,
        expenses = 3100.0,
        saved = 1100.0,
        expensesPercentOfIncome = 74,
        savedPercentOfIncome = 26,
    )

    private fun setContent(
        uiState: OverviewUiState = OverviewUiState.Success(sampleSummary),
        selectedPeriod: OverviewPeriod = OverviewPeriod.THIS_MONTH,
        onPeriodSelected: (OverviewPeriod) -> Unit = {},
    ) {
        composeTestRule.setContent {
            LedgerTheme {
                OverviewContent(
                    uiState = uiState,
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    // ── Period selector (AC3) ─────────────────────────────────────────────────────

    @Test
    fun periodSelector_displaysChips() {
        setContent()

        composeTestRule.onNodeWithText("This Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 Months").assertIsDisplayed()
    }

    @Test
    fun periodSelector_selectedChipIsMarkedSelected() {
        setContent(selectedPeriod = OverviewPeriod.THIS_MONTH)

        composeTestRule.onNodeWithText("This Month").assertIsSelected()
    }

    @Test
    fun periodSelector_tappingChip_invokesCallbackWithThatPeriod() {
        var selected: OverviewPeriod? = null
        setContent(onPeriodSelected = { selected = it })

        composeTestRule.onNodeWithText("Last Month").performClick()

        assertEquals(OverviewPeriod.LAST_MONTH, selected)
    }

    // ── Summary card (AC1, AC2, AC6) ──────────────────────────────────────────────

    @Test
    fun summaryCard_displaysIncomeExpensesAndSaved() {
        setContent()

        // Labels (rendered uppercase by the eyebrow).
        composeTestRule.onNodeWithText("INCOME").assertIsDisplayed()
        composeTestRule.onNodeWithText("EXPENSES").assertIsDisplayed()
        composeTestRule.onNodeWithText("SAVED").assertIsDisplayed()
        // Formatted dollar amounts.
        composeTestRule.onNodeWithText("$4,200.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("$3,100.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("$1,100.00").assertIsDisplayed()
    }

    @Test
    fun summaryCard_displaysPercentOfIncomeForExpensesAndSaved() {
        setContent()

        composeTestRule.onNodeWithText("74% of income").assertIsDisplayed()
        composeTestRule.onNodeWithText("26% of income").assertIsDisplayed()
    }

    @Test
    fun summaryCard_zeroIncome_hidesPercentOfIncome() {
        setContent(
            uiState = OverviewUiState.Success(
                OverviewSummary(
                    income = 0.0,
                    expenses = 0.0,
                    saved = 0.0,
                    expensesPercentOfIncome = null,
                    savedPercentOfIncome = null,
                ),
            ),
        )

        // Null percentages render nothing rather than "0% of income".
        composeTestRule.onNodeWithText("of income", substring = true).assertDoesNotExist()
    }

    @Test
    fun summaryCard_negativeSaved_displaysNegativeAmount() {
        // Color (AC6 red) can't be asserted via semantics; the negative value rendering is the
        // observable signal that the negative-saved branch is active.
        setContent(
            uiState = OverviewUiState.Success(
                OverviewSummary(
                    income = 3100.0,
                    expenses = 4200.0,
                    saved = -1100.0,
                    expensesPercentOfIncome = 135,
                    savedPercentOfIncome = -35,
                ),
            ),
        )

        composeTestRule.onNodeWithText("-$1,100.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("-35% of income").assertIsDisplayed()
    }

    // ── Loading / error states ────────────────────────────────────────────────────

    @Test
    fun loadingState_isShown() {
        setContent(uiState = OverviewUiState.Loading)

        composeTestRule.onNodeWithContentDescription("Loading summary...").assertIsDisplayed()
    }

    @Test
    fun errorState_showsMessage() {
        setContent(uiState = OverviewUiState.Error("Database unavailable"))

        composeTestRule.onNodeWithText("Database unavailable").assertIsDisplayed()
    }
}
