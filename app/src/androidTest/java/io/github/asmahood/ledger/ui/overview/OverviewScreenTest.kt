package io.github.asmahood.ledger.ui.overview

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val sampleChart = CategorySpendChart(
        monthLabels = listOf("Jan '26", "Feb '26", "Mar '26"),
        series = listOf(
            CategorySeries(
                categoryId = 1,
                categoryName = "Gas",
                amounts = listOf(56.0, 45.85, 73.22),
            ),
            CategorySeries(
                categoryId = 2,
                categoryName = "Rent",
                amounts = listOf(1500.0, 1500.0, 1500.0),
            ),
        ),
    )

    private val sampleIncomeChart = TotalIncomeChart(
        monthLabels = listOf("Jan '26", "Feb '26", "Mar '26"),
        amounts = listOf(4000.0, 3950.0, 4600.0),
    )

    private fun success(
        summary: OverviewSummary = sampleSummary,
        chart: CategorySpendChart = sampleChart,
        incomeChart: TotalIncomeChart = sampleIncomeChart,
    ) = OverviewUiState.Success(summary = summary, categorySpendChart = chart, totalIncomeChart = incomeChart)

    private fun setContent(
        uiState: OverviewUiState = success(),
        selectedPeriod: OverviewPeriod = OverviewPeriod.THIS_MONTH,
        onPeriodSelected: (OverviewPeriod) -> Unit = {},
        onCategoryToggled: (Long) -> Unit = {},
    ) {
        composeTestRule.setContent {
            LedgerTheme {
                OverviewContent(
                    uiState = uiState,
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected,
                    onCategoryToggled = onCategoryToggled,
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
            uiState = success(
                summary = OverviewSummary(
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
            uiState = success(
                summary = OverviewSummary(
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

    // ── Total income chart (AC1, AC4) ─────────────────────────────────────────────

    @Test
    fun totalIncomeChart_cardRenders() {
        setContent()

        composeTestRule.onNodeWithTag(TotalIncomeChartTestTag).assertIsDisplayed()
    }

    @Test
    fun totalIncomeChart_zeroFilledMonths_stillRendersCard() {
        // AC4: months with no income data map to zero-filled amounts rather than being omitted,
        // so the card must still render without crashing when every amount is zero.
        setContent(
            uiState = success(
                incomeChart = TotalIncomeChart(
                    monthLabels = listOf("Jan '26"),
                    amounts = listOf(0.0),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TotalIncomeChartTestTag).assertIsDisplayed()
    }

    // ── Category spend chart ──────────────────────────────────────────────────────

    @Test
    fun categoryChart_displaysTitle() {
        setContent()

        // SummaryEyebrow renders its label uppercase, matching the summary-card eyebrows.
        composeTestRule.onNodeWithText("EXPENSES BY CATEGORY").assertIsDisplayed()
    }

    @Test
    fun categoryChart_legend_showsVisibleCategoryNames() {
        setContent()

        // Legend labels come from the visible series. The legend sits below the income chart, so
        // scroll it into view before asserting.
        composeTestRule.onNodeWithText("Gas").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Rent").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun categoryChart_openingMenu_andTappingCategory_invokesToggleWithThatId() {
        var toggled: Long? = null
        setContent(onCategoryToggled = { toggled = it })

        composeTestRule.onNodeWithText("Categories").performClick()
        // "Rent" now appears both in the legend and the open menu; tapping the menu row toggles it.
        composeTestRule.onAllNodesWithText("Rent").onLast().performClick()

        assertEquals(2L, toggled)
    }

    @Test
    fun categoryChart_hiddenCategory_isOmittedFromLegend() {
        setContent(
            uiState = success(
                chart = sampleChart.copy(
                    series = sampleChart.series.map { series ->
                        if (series.categoryId == 1L) series.copy(isVisible = false) else series
                    },
                ),
            ),
        )

        // Gas is hidden, so it should not appear as a legend entry. Rent stays visible.
        composeTestRule.onNodeWithText("Rent").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Gas").fetchSemanticsNodes().let { nodes ->
            assertTrue("Hidden category should not render a legend label", nodes.isEmpty())
        }
    }

    @Test
    fun categoryChart_noCategories_hidesMenu() {
        setContent(
            uiState = success(
                chart = CategorySpendChart(
                    monthLabels = listOf("Jan '26"),
                    series = emptyList(),
                ),
            ),
        )

        // With no series there is nothing to filter, so the Categories menu button is absent.
        composeTestRule.onAllNodesWithText("Categories").fetchSemanticsNodes().let { nodes ->
            assertTrue("Menu button should be hidden when there are no categories", nodes.isEmpty())
        }
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
