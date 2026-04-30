package com.saadm.zenith

import android.R.attr.type
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saadm.zenith.data.preferences.AppPreferences
import com.saadm.zenith.data.preferences.AppPreferencesStore
import com.saadm.zenith.ui.components.NavRoute
import com.saadm.zenith.ui.components.NavPillBar
import com.saadm.zenith.ui.components.PrimaryActionSpec
import com.saadm.zenith.ui.components.navItems
import com.saadm.zenith.ui.home.HomeScreen
import com.saadm.zenith.ui.insights.InsightsScreen
import com.saadm.zenith.ui.settings.SettingsScreen
import com.saadm.zenith.ui.settings.TransitionStyle
import com.saadm.zenith.ui.settings.normalizeTransitionDurationMillis
import com.saadm.zenith.ui.theme.ZenithTheme
import com.saadm.zenith.ui.add.AddTransactionFlow
import com.saadm.zenith.ui.components.payeeDetailsRoute
import com.saadm.zenith.ui.people.PeopleScreen
import com.saadm.zenith.ui.people.PeopleViewModel
import com.saadm.zenith.ui.people.PayeeDetailsRoute
import dagger.hilt.android.AndroidEntryPoint

private fun bottomNavIndex(route: String?): Int? {
    val index = navItems.indexOfFirst { it.route.route == route }
    return index.takeIf { it >= 0 }
}

private fun slideDirectionFor(
    initialRoute: String?,
    targetRoute: String?
): SlideDirection? {
    val initialIndex = bottomNavIndex(initialRoute) ?: return null
    val targetIndex = bottomNavIndex(targetRoute) ?: return null

    if (initialIndex == targetIndex) return null

    return if (targetIndex > initialIndex) {
        SlideDirection.Left
    } else {
        SlideDirection.Right
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenithTheme {
                val appPreferencesStore = remember { AppPreferencesStore(applicationContext) }
                val appPreferences by appPreferencesStore.preferencesFlow.collectAsState(
                    initial = AppPreferences()
                )
                val transitionDurationMillis = normalizeTransitionDurationMillis(
                    appPreferences.transitionDurationMillis
                )
                val transitionStyle = appPreferences.transitionStyle
                var showAddTransactionSheet by rememberSaveable { mutableStateOf(false) }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: NavRoute.Home.route
                val primaryAction = remember(currentRoute) {
                    when (currentRoute) {
                        NavRoute.Home.route -> PrimaryActionSpec(
                            icon = Icons.Rounded.Add,
                            contentDescription = "Add transaction",
                            onClick = { showAddTransactionSheet = true }
                        )

                        NavRoute.People.route -> PrimaryActionSpec(
                            icon = Icons.Rounded.PersonAdd,
                            contentDescription = "Add payee",
                            onClick = { println("[people] add payee") }
                        )

                        else -> PrimaryActionSpec(
                            icon = Icons.Rounded.Add,
                            contentDescription = "Add transaction",
                            onClick = { showAddTransactionSheet = true }
//                            icon = Icons.Rounded.Tune,
//                            contentDescription = "Open quick actions",
//                            onClick = { println("Settings action: open bottom sheet") }
                        )
                    }
                }

                // Hide bottom navigation for payee details (modal) route
                val showBottomBar = currentRoute?.startsWith("payee_details") != true

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavPillBar(
                                navController = navController,
                                currentRoute = currentRoute,
                                primaryAction = primaryAction
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoute.Home.route,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            // If navigating to the payee details route, present it as a modal that
                            // slides up from the bottom. Otherwise fall back to horizontal
                            // slide transitions used by the bottom nav.
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
                            // When navigating to the modal payee details, keep the underlying
                            // screen static (no exit animation) so the modal appears above it.
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
                            // When returning (pop) to a destination, if the target is payee details
                            // we don't need to animate the underlying content — the modal handles its own pop exit.
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
                            // If the initial route is the payee details modal, pop should slide the modal
                            // downwards and reveal the underlying screen.
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
                        composable(NavRoute.Home.route) { HomeScreen() }
//                         composable(NavRoute.Transactions.route) { TransactionsScreen() }
                        composable(NavRoute.People.route) {
                            // Pass a navigation callback that navigates using the payee details route.
                            PeopleScreen({ id -> navController.navigate(payeeDetailsRoute(id)) })
                        }
                        composable(NavRoute.Insights.route) { InsightsScreen() }
                        composable(NavRoute.Settings.route) {
                            SettingsScreen({ id -> navController.navigate(payeeDetailsRoute(id)) })
                        }
                        // Payee details modal route: receives only the payeeId and fetches its own data
                        composable(
                            route = NavRoute.PayeeDetails.route,
                            arguments = listOf(navArgument("payeeId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getLong("payeeId") ?: return@composable
                            val peopleViewModel = hiltViewModel<PeopleViewModel>()
                            // Wrap details in a simple top bar that offers an explicit back action;
                            // the modal's enter/popExit transitions are handled by NavHost above.
                            com.saadm.zenith.ui.people.PayeeDetailsRoute(
                                payeeId = id,
                                peopleViewModel = peopleViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    if (showAddTransactionSheet) {
                        AddTransactionFlow(
                            onDismissRequest = { showAddTransactionSheet = false }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    ZenithTheme {
        HomeScreen()
    }
}
