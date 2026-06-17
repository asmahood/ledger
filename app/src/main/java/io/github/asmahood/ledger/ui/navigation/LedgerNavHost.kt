package io.github.asmahood.ledger.ui.navigation

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.ui.manage.ManageScreen
import io.github.asmahood.ledger.ui.overview.OverviewScreen
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.ui.transaction.TransactionScreen

@Composable
fun LedgerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Overview.route,
        modifier = modifier
    ) {
        composable(route = Screen.Overview.route) {
            OverviewScreen()
        }
        composable(route = Screen.Transactions.route) {
            TransactionScreen()
        }
        composable(route = Screen.Manage.route) {
            ManageScreen()
        }
    }
}

@PreviewLightDark
@Composable
private fun LedgerNavHostPreview() {
    LedgerTheme {
        LedgerNavHost(navController = rememberNavController())
    }
}

@Composable
fun LedgerBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(modifier = modifier) {
        Screen.tabs.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(route = screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(if (isSelected) screen.iconActive else screen.icon),
                        contentDescription = stringResource(screen.title)
                    )
                },
                label = { Text(stringResource(screen.title)) }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LedgerBottomBarPreview() {
    LedgerTheme {
        LedgerBottomBar(navController = rememberNavController())
    }
}

@Composable
fun FAB(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = stringResource(R.string.fab_desc)
        )
    }
}

@PreviewLightDark
@Composable
private fun FabPreview() {
    LedgerTheme {
        FAB(onClick = {})
    }
}

