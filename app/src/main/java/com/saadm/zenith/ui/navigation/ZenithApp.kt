package com.saadm.zenith.ui.navigation

import com.saadm.zenith.ui.common.LocalBottomBarInset
import androidx.compose.runtime.CompositionLocalProvider
import com.saadm.zenith.ui.components.Lu
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saadm.zenith.ui.components.NavPillBar
import com.saadm.zenith.ui.components.PillTab
import com.saadm.zenith.ui.components.PrimaryActionSpec
import com.saadm.zenith.ui.settings.TransitionStyle

private val pillTabs = listOf(
    PillTab(Destination.Overview.route, "Overview", Lu.lucide_ic_house),
    PillTab(Destination.Insights.route, "Insights", Lu.lucide_ic_chart_pie),
    PillTab(Destination.Settings.route, "Settings", Lu.lucide_ic_settings),
)

/** Scaffold with the floating pill bar, over the route graph. */
@Composable
fun ZenithApp(transitionDurationMillis: Int, transitionStyle: TransitionStyle) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.Overview.route
    // The action on the Payees screen bumps this; the screen opens its add dialog on each change.
    var newPayeeRequests by rememberSaveable { mutableIntStateOf(0) }

    val primaryAction = if (currentRoute == Destination.Payees.route) {
        PrimaryActionSpec(
            icon = Lu.lucide_ic_user_plus,
            contentDescription = "Add payee",
            onClick = { newPayeeRequests++ }
        )
    } else {
        PrimaryActionSpec(
            icon = Lu.lucide_ic_plus,
            contentDescription = "Add transaction",
            onClick = { navController.navigate(Destination.ADD_ROUTE) { launchSingleTop = true } }
        )
    }

    Scaffold(
        bottomBar = {
            if (!Destination.isFullScreen(currentRoute)) {
                NavPillBar(
                    tabs = pillTabs,
                    selectedRoute = Destination.tabFor(currentRoute),
                    onTabSelected = { route -> navController.navigateToTab(route) },
                    primaryAction = primaryAction
                )
            }
        }
    ) { innerPadding ->
        // Only the top is padded: content scrolls behind the floating pill.
        CompositionLocalProvider(LocalBottomBarInset provides innerPadding.calculateBottomPadding()) {
            ZenithNavHost(
                navController = navController,
                newPayeeRequests = newPayeeRequests,
                transitionDurationMillis = transitionDurationMillis,
                transitionStyle = transitionStyle,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
            )
        }
    }
}

// Tab switches reset the stack to the start destination, so Back from a tab returns to Overview.
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
