package io.github.asmahood.ledger.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {

    @Test
    fun tabs_containsCorrectDestinationsInOrder() {
        assertEquals(
            listOf(Screen.Overview, Screen.Transactions, Screen.Manage),
            Screen.tabs
        )
    }

    @Test
    fun tabs_routesAreUnique() {
        val routes = Screen.tabs.map { it.route }
        assertEquals("Each tab must have a unique route", routes.distinct().size, routes.size)
    }

    @Test
    fun fullScreenRoutes_containsAddAndEditCategory() {
        assertTrue(Screen.fullScreenRoutes.contains(Screen.AddCategory.route))
        assertTrue(Screen.fullScreenRoutes.contains(Screen.EditCategory.route))
    }

    @Test
    fun editCategory_routeContainsCategoryIdArgument() {
        assertTrue(Screen.EditCategory.route.contains("{categoryId}"))
    }

    @Test
    fun fullScreenRoutes_containsAddAndEditTransaction() {
        assertTrue(Screen.fullScreenRoutes.contains(Screen.AddTransaction.route))
        assertTrue(Screen.fullScreenRoutes.contains(Screen.EditTransaction.route))
    }

    @Test
    fun editTransaction_routeContainsTransactionIdArgument() {
        assertTrue(Screen.EditTransaction.route.contains("{transactionId}"))
    }

    @Test
    fun importTransactions_isAFullScreenRoute() {
        assertTrue(Screen.ImportTransactions.route in Screen.fullScreenRoutes)
    }

    @Test
    fun importTransactions_isNotATab() {
        assertFalse(Screen.ImportTransactions in Screen.tabs)
    }

    @Test
    fun importTransactions_hasNoFab() {
        assertFalse(Screen.ImportTransactions.route in Screen.routesWithFAB)
    }
}
