package io.github.asmahood.ledger.ui.navigation

import org.junit.Assert.assertEquals
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
}
