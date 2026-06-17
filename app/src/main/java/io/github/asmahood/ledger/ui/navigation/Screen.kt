package io.github.asmahood.ledger.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.asmahood.ledger.R

sealed class Screen(
    val route: String,
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val iconActive: Int,
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

    companion object {
        val tabs: List<Screen> by lazy { listOf(Overview, Transactions, Manage) }
    }
}