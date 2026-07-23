package io.github.asmahood.ledger.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.asmahood.ledger.R

sealed class Screen(
    val route: String,
    @StringRes val title: Int,
    @DrawableRes val icon: Int? = null,
    @DrawableRes val iconActive: Int? = null,
) {
    data object Overview : Screen(
        route = "overview",
        title = R.string.overview,
        icon = R.drawable.pie_chart,
        iconActive = R.drawable.pie_chart_filled,
    )

    data object Transactions : Screen(
        route = "transactions",
        title = R.string.transactions,
        icon = R.drawable.receipt,
        iconActive = R.drawable.receipt_filled,
    )

    data object Manage : Screen(
        route = "manage",
        title = R.string.manage,
        icon = R.drawable.tune,
        iconActive = R.drawable.tune_filled,
    )

    data object AddCategory : Screen(
        route = "addCategory",
        title = R.string.add_category,
    )

    data object EditCategory : Screen(
        route = "editCategory/{categoryId}",
        title = R.string.edit_category,
    )

    data object AddTransaction : Screen(
        route = "addTransaction",
        title = R.string.add_transaction,
    )

    data object EditTransaction : Screen(
        route = "editTransaction/{transactionId}",
        title = R.string.edit_transaction
    )

    data object ImportTransactions : Screen(
        route = "importTransactions",
        title = R.string.import_transactions,
    )

    companion object {
        val tabs: List<Screen> by lazy { listOf(Overview, Transactions, Manage) }
        val fullScreenRoutes: Set<String> by lazy {
            setOf(
                AddCategory.route, EditCategory.route,
                AddTransaction.route, EditTransaction.route, ImportTransactions.route
            )
        }
        val routesWithFAB: Set<String> by lazy { setOf(Overview.route, Transactions.route) }
    }
}