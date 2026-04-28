package com.saadm.zenith

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

                Scaffold(
                    bottomBar = {
                        NavPillBar(
                            navController = navController,
                            currentRoute = currentRoute,
                            primaryAction = primaryAction
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoute.Home.route,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
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
                        },
                        exitTransition = {
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
                        },
                        popEnterTransition = {
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
                        },
                        popExitTransition = {
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
                     ) {
                         composable(NavRoute.Home.route) { HomeScreen() }
//                         composable(NavRoute.Transactions.route) { TransactionsScreen() }
//                         composable(NavRoute.People.route) { PeopleScreen() }
                        composable(NavRoute.Insights.route) { InsightsScreen() }
                         composable(NavRoute.Settings.route) {
                             SettingsScreen()
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
