package com.saadm.zenith.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.saadm.zenith.ui.add.AddTransactionFlow
import com.saadm.zenith.ui.components.EmptyState
import com.saadm.zenith.ui.home.HomeScreen
import com.saadm.zenith.ui.insights.InsightsScreen
import com.saadm.zenith.ui.people.PayeeDetailsRoute
import com.saadm.zenith.ui.people.PeopleScreen
import com.saadm.zenith.ui.people.PeopleViewModel
import com.saadm.zenith.ui.settings.SettingsScreen
import com.saadm.zenith.ui.settings.TransitionStyle
import com.saadm.zenith.ui.transactions.TransactionsScreen

private fun topLevelTabIndex(route: String?): Int? =
    Destination.topLevelTabs.indexOfFirst { it.route == route }.takeIf { it >= 0 }

private fun slideDirectionFor(initialRoute: String?, targetRoute: String?): SlideDirection? {
    val initialIndex = topLevelTabIndex(initialRoute) ?: return null
    val targetIndex = topLevelTabIndex(targetRoute) ?: return null

    if (initialIndex == targetIndex) return null

    return if (targetIndex > initialIndex) SlideDirection.Left else SlideDirection.Right
}

/**
 * The route graph. Tabs slide sideways, payee details rises as a modal, and Add is a dialog.
 * [newPayeeRequests] is bumped by the Payees action.
 */
@Composable
fun ZenithNavHost(
    navController: NavHostController,
    newPayeeRequests: Int,
    transitionDurationMillis: Int,
    transitionStyle: TransitionStyle,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Overview.route,
        modifier = modifier,
        enterTransition = {
            // Payee details presents as a modal that slides up. Other routes use the tab slide.
            val targetRoute = targetState.destination.route
            if (targetRoute?.startsWith("payee_details") == true) {
                val slide = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(durationMillis = transitionDurationMillis))
                if (transitionStyle == TransitionStyle.FadeSlide) slide + fadeIn(animationSpec = tween(durationMillis = transitionDurationMillis)) else slide
            } else {
                val direction = slideDirectionFor(
                    initialRoute = initialState.destination.route,
                    targetRoute = targetState.destination.route
                )
                if (direction == null) {
                    EnterTransition.None
                } else {
                    val slideTransition = slideIntoContainer(
                        towards = direction,
                        animationSpec = tween(durationMillis = transitionDurationMillis)
                    )
                    if (transitionStyle == TransitionStyle.FadeSlide) {
                        slideTransition + fadeIn(animationSpec = tween(durationMillis = transitionDurationMillis))
                    } else {
                        slideTransition
                    }
                }
            }
        },
        exitTransition = {
            // Under the payee modal the screen stays still, so the modal appears above it.
            val targetRoute = targetState.destination.route
            if (targetRoute?.startsWith("payee_details") == true) {
                ExitTransition.None
            } else {
                val direction = slideDirectionFor(
                    initialRoute = initialState.destination.route,
                    targetRoute = targetState.destination.route
                )
                if (direction == null) {
                    ExitTransition.None
                } else {
                    val slideTransition = slideOutOfContainer(
                        towards = direction,
                        animationSpec = tween(durationMillis = transitionDurationMillis)
                    )
                    if (transitionStyle == TransitionStyle.FadeSlide) {
                        slideTransition + fadeOut(animationSpec = tween(durationMillis = transitionDurationMillis))
                    } else {
                        slideTransition
                    }
                }
            }
        },
        popEnterTransition = {
            // Pop back to the payee modal: the modal animates its own exit, so the screen does not.
            val targetRoute = targetState.destination.route
            if (targetRoute?.startsWith("payee_details") == true) {
                EnterTransition.None
            } else {
                val direction = slideDirectionFor(
                    initialRoute = initialState.destination.route,
                    targetRoute = targetState.destination.route
                )
                if (direction == null) {
                    EnterTransition.None
                } else {
                    val slideTransition = slideIntoContainer(
                        towards = direction,
                        animationSpec = tween(durationMillis = transitionDurationMillis)
                    )
                    if (transitionStyle == TransitionStyle.FadeSlide) {
                        slideTransition + fadeIn(animationSpec = tween(durationMillis = transitionDurationMillis))
                    } else {
                        slideTransition
                    }
                }
            }
        },
        popExitTransition = {
            // Popping the payee modal slides it down and reveals the screen underneath.
            val initialRoute = initialState.destination.route
            if (initialRoute?.startsWith("payee_details") == true) {
                val slide = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }, animationSpec = tween(durationMillis = transitionDurationMillis))
                if (transitionStyle == TransitionStyle.FadeSlide) slide + fadeOut(animationSpec = tween(durationMillis = transitionDurationMillis)) else slide
            } else {
                val direction = slideDirectionFor(
                    initialRoute = initialState.destination.route,
                    targetRoute = targetState.destination.route
                )
                if (direction == null) {
                    ExitTransition.None
                } else {
                    val slideTransition = slideOutOfContainer(
                        towards = direction,
                        animationSpec = tween(durationMillis = transitionDurationMillis)
                    )
                    if (transitionStyle == TransitionStyle.FadeSlide) {
                        slideTransition + fadeOut(animationSpec = tween(durationMillis = transitionDurationMillis))
                    } else {
                        slideTransition
                    }
                }
            }
        }
    ) {
        val openPayee: (Long) -> Unit = { id -> navController.navigate(Destination.payeeDetailsRoute(id)) }

        composable(Destination.Overview.route) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Destination.Settings.route) { launchSingleTop = true } },
                onOpenTransactions = { navController.navigate(Destination.Transactions.route) },
                onOpenPayees = { navController.navigate(Destination.Payees.route) }
            )
        }
        composable(Destination.Insights.route) {
            InsightsScreen(
                onOpenBudgets = { navController.navigate(Destination.Budgets.route) },
                onOpenPayees = { navController.navigate(Destination.Payees.route) },
                onOpenCategories = { navController.navigate(Destination.Categories.route) }
            )
        }
        composable(Destination.Settings.route) { SettingsScreen(onNavigateToPayee = openPayee) }
        composable(Destination.Budgets.route) { PlaceholderScreen("Budgets") }
        composable(Destination.Transactions.route) { TransactionsScreen() }
        composable(Destination.Payees.route) {
            PeopleScreen(onNavigateToPayee = openPayee, createRequests = newPayeeRequests)
        }
        composable(Destination.Categories.route) { PlaceholderScreen("Categories") }
        composable(
            route = Destination.PayeeDetails.route,
            arguments = listOf(navArgument(PAYEE_ID_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(PAYEE_ID_ARG) ?: return@composable
            PayeeDetailsRoute(
                payeeId = id,
                peopleViewModel = hiltViewModel<PeopleViewModel>(),
                onBack = { navController.popBackStack() }
            )
        }
        dialog(Destination.AddTransaction.route) {
            AddTransactionFlow(onDismissRequest = { navController.popBackStack() })
        }
    }
}

// Shown for routes whose screens land in later work. The route is reachable; the content is not built yet.
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(title = title, body = "This screen is not built yet.")
    }
}
