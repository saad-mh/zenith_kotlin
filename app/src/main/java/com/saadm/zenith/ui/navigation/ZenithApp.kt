package com.saadm.zenith.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saadm.zenith.ui.common.LocalBottomBarInset
import com.saadm.zenith.ui.components.Lu
import com.saadm.zenith.ui.components.NavPillBar
import com.saadm.zenith.ui.components.PillTab
import com.saadm.zenith.ui.components.PrimaryActionSpec
import com.saadm.zenith.ui.home.HomeScreen
import com.saadm.zenith.ui.insights.InsightsScreen
import com.saadm.zenith.ui.settings.SettingsPage
import com.saadm.zenith.ui.settings.SettingsScreen
import com.saadm.zenith.ui.settings.TransitionStyle
import com.saadm.zenith.ui.theme.zenithColors

private val pillTabs = listOf(
    PillTab(Destination.Overview.route, "Overview", Lu.lucide_ic_house),
    PillTab(Destination.Insights.route, "Insights", Lu.lucide_ic_chart_pie),
    PillTab(Destination.Settings.route, "Settings", Lu.lucide_ic_settings),
)

/** The screens a tab page can push. */
class Pushes(
    val add: () -> Unit,
    val transactions: () -> Unit,
    val payees: () -> Unit,
    val budgets: () -> Unit,
    val categories: () -> Unit,
    val settingsPage: (SettingsPage) -> Unit
)

private fun tabIndex(entry: NavBackStackEntry): Int =
    Destination.topLevelTabs.indexOfFirst { it.route == entry.destination.route }

// A tab to the right in the pill comes in from the right, and the other way round.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.towards(): SlideDirection =
    if (tabIndex(targetState) > tabIndex(initialState)) SlideDirection.Left else SlideDirection.Right

/**
 * The three tabs with the floating pill. They are one page of the outer stack, so a pushed screen
 * slides in over the tab and the pill together. Tabs keep their own history: Back returns to the
 * tab you came from, and tapping a tab always shows that tab's own screen.
 */
@Composable
fun TabsPage(pushes: Pushes, transitionDurationMillis: Int, transitionStyle: TransitionStyle) {
    val tabs = rememberNavController()
    val entry by tabs.currentBackStackEntryAsState()
    val current = entry?.destination?.route ?: Destination.Overview.route
    val select: (String) -> Unit = { route -> tabs.navigate(route) { launchSingleTop = true } }
    val spec = tween<androidx.compose.ui.unit.IntOffset>(transitionDurationMillis)
    val fades = transitionStyle == TransitionStyle.FadeSlide
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(towards(), spec) + if (fades) fadeIn(tween(transitionDurationMillis)) else EnterTransition.None
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(towards(), spec) + if (fades) fadeOut(tween(transitionDurationMillis)) else ExitTransition.None
    }

    Scaffold(
        containerColor = zenithColors().surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        bottomBar = {
            NavPillBar(
                tabs = pillTabs,
                selectedRoute = current,
                onTabSelected = select,
                primaryAction = PrimaryActionSpec(Lu.lucide_ic_plus, "Add transaction", pushes.add)
            )
        }
    ) { innerPadding ->
        // Only the top is padded: content scrolls behind the floating pill.
        CompositionLocalProvider(LocalBottomBarInset provides innerPadding.calculateBottomPadding()) {
            NavHost(
                navController = tabs,
                startDestination = Destination.Overview.route,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = enter,
                popExitTransition = exit,
                sizeTransform = null
            ) {
                composable(Destination.Overview.route) {
                    HomeScreen(
                        onOpenSettings = { select(Destination.Settings.route) },
                        onOpenTransactions = pushes.transactions,
                        onOpenPayees = pushes.payees,
                        onAdd = pushes.add
                    )
                }
                composable(Destination.Insights.route) {
                    InsightsScreen(
                        onOpenBudgets = pushes.budgets,
                        onOpenPayees = pushes.payees,
                        onOpenCategories = pushes.categories,
                        onAdd = pushes.add
                    )
                }
                composable(Destination.Settings.route) {
                    SettingsScreen(
                        onOpenPage = pushes.settingsPage,
                        onOpenCategories = pushes.categories,
                        onOpenPayees = pushes.payees,
                        onOpenBudgets = pushes.budgets
                    )
                }
            }
        }
    }
}
