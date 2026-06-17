package io.github.asmahood.ledger.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerBottomBarTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    private fun setupContent() {
        composeTestRule.setContent {
            navController = buildTestNavController()
            LedgerTheme { LedgerBottomBar(navController = navController) }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomBar_displaysAllThreeTabs() {
        setupContent()

        composeTestRule.onNodeWithText("Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage").assertIsDisplayed()
    }

    @Test
    fun bottomBar_overviewIsSelectedOnStart() {
        setupContent()

        composeTestRule.onNodeWithText("Overview").assertIsSelected()
    }

    @Test
    fun bottomBar_clickingTabNavigatesToCorrectRoute() {
        setupContent()

        composeTestRule.onNodeWithText("Transactions").performClick()

        assertEquals(
            Screen.Transactions.route,
            navController.currentBackStackEntry?.destination?.route
        )
    }
}

@Composable
private fun buildTestNavController(): TestNavHostController {
    val context = LocalContext.current
    return remember {
        TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = Screen.Overview.route) {
                Screen.tabs.forEach { screen -> composable(screen.route) {} }
            }
        }
    }
}
