package com.saadm.zenith.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationTest {
    // Touching a tab object first is what the app does on launch, and what used to leave a null in the list.
    @Test
    fun topLevelTabsHaveNoNullsWhenATabIsTouchedFirst() {
        val first = Destination.Overview.route
        assertEquals(listOf(first, "insights", "settings"), Destination.topLevelTabs.map { it.route })
    }
}
