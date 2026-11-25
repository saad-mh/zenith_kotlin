package com.saadm.zenith.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saadm.zenith.ui.add.AddTransactionFlow
import com.saadm.zenith.ui.budgets.BudgetsScreen
import com.saadm.zenith.ui.categories.CategoriesScreen
import com.saadm.zenith.ui.common.LocalBottomBarInset
import com.saadm.zenith.ui.people.PayeeDetailsScreen
import com.saadm.zenith.ui.people.PeopleScreen
import com.saadm.zenith.ui.settings.SettingsPage
import com.saadm.zenith.ui.settings.SettingsPageScreen
import com.saadm.zenith.ui.settings.TransitionStyle
import com.saadm.zenith.ui.theme.zenithColors
import com.saadm.zenith.ui.transactions.TransactionsScreen

/** A pushed screen: its own background, so it moves as one sheet, and room for the system bars. */
@Composable
private fun Page(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(zenithColors().surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        CompositionLocalProvider(LocalBottomBarInset provides bottom, content = content)
    }
}

/**
 * The app's navigation stack: the tabs page at the bottom and every other screen pushed on top of it.
 * A push slides in from the right while the page under it leaves to the left; Back reverses it.
 */
@Composable
fun ZenithApp(transitionDurationMillis: Int, transitionStyle: TransitionStyle) {
    val navController = rememberNavController()
    val spec = tween<IntOffset>(transitionDurationMillis)
    val fadeSpec = tween<Float>(transitionDurationMillis)
    val fades = transitionStyle == TransitionStyle.FadeSlide

    fun push(route: String) = navController.navigate(route) { launchSingleTop = true }
    // Ignored while a screen is still moving, so a double tap cannot pop two screens.
    val back: () -> Unit = {
        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
            navController.popBackStack()
        }
    }
    val pushes = Pushes(
        add = { push(Destination.ADD_ROUTE) },
        transactions = { push(Destination.Transactions.route) },
        payees = { push(Destination.Payees.route) },
        budgets = { push(Destination.Budgets.route) },
        categories = { push(Destination.Categories.route) },
        settingsPage = { page -> push(Destination.settingsRoute(page.name)) }
    )

    NavHost(
        navController = navController,
        startDestination = Destination.Tabs.route,
        modifier = Modifier.fillMaxSize().background(zenithColors().surface),
        enterTransition = {
            slideIntoContainer(SlideDirection.Left, spec) + if (fades) fadeIn(fadeSpec) else EnterTransition.None
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Left, spec) + if (fades) fadeOut(fadeSpec) else ExitTransition.None
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.Right, spec) + if (fades) fadeIn(fadeSpec) else EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.Right, spec) + if (fades) fadeOut(fadeSpec) else ExitTransition.None
        },
        sizeTransform = null
    ) {
        composable(Destination.Tabs.route) { TabsPage(pushes, transitionDurationMillis, transitionStyle) }
        composable(Destination.SettingsDetail.route) { entry ->
            val name = entry.arguments?.getString(SETTINGS_PAGE_ARG)
            val page = SettingsPage.entries.firstOrNull { it.name == name } ?: SettingsPage.Appearance
            Page { SettingsPageScreen(page, onBack = back) }
        }
        composable(Destination.Budgets.route) { Page { BudgetsScreen(onBack = back) } }
        composable(Destination.Transactions.route) {
            Page {
                TransactionsScreen(
                    onBack = back,
                    onAdd = pushes.add,
                    onEdit = { id -> push(Destination.editRoute(id)) },
                    onRepeat = { id -> push(Destination.repeatRoute(id)) }
                )
            }
        }
        composable(Destination.Payees.route) {
            Page {
                PeopleScreen(
                    onNavigateToPayee = { id -> push(Destination.payeeDetailsRoute(id)) },
                    onBack = back
                )
            }
        }
        composable(Destination.Categories.route) {
            Page {
                CategoriesScreen(
                    onBack = back,
                    onOpenDefault = { pushes.settingsPage(SettingsPage.DefaultCategory) }
                )
            }
        }
        composable(
            route = Destination.PayeeDetails.route,
            arguments = listOf(navArgument(PAYEE_ID_ARG) { type = NavType.LongType })
        ) {
            // Not [back]: removing a person calls this when the write finishes, whatever the screen is doing.
            Page { PayeeDetailsScreen(onBack = { navController.popBackStack() }) }
        }
        dialog(
            route = Destination.AddTransaction.route,
            arguments = listOf(
                navArgument(TXN_ID_ARG) { type = NavType.LongType; defaultValue = 0L },
                navArgument(REPEAT_ARG) { type = NavType.BoolType; defaultValue = false }
            )
        ) {
            AddTransactionFlow(onDismissRequest = { navController.popBackStack() })
        }
    }
}
