package io.github.asmahood.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.asmahood.ledger.ui.navigation.FAB
import io.github.asmahood.ledger.ui.navigation.LedgerBottomBar
import io.github.asmahood.ledger.ui.navigation.LedgerNavHost
import io.github.asmahood.ledger.ui.navigation.Screen
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerApp()
        }
    }
}

@Composable
fun LedgerApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute !in Screen.fullScreenRoutes
    val showFAB = currentRoute in Screen.routesWithFAB

    LedgerTheme {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    LedgerBottomBar(navController = navController)
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFAB,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    FAB(onClick = { navController.navigate(Screen.AddTransaction.route) })
                }
            },
            modifier = modifier
        ) { contentPadding ->
            // The shell handles the bottom edge (bottom bar + nav-bar inset). We pad
            // the NavHost by that, and consumeWindowInsets tells each screen's own
            // Scaffold those insets are already handled — so their top bars don't
            // re-apply the status-bar inset and double the top padding.
            LedgerNavHost(
                navController = navController,
                modifier = Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LedgerAppPreview() {
    LedgerApp()
}