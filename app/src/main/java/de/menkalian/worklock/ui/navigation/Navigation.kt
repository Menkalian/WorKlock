package de.menkalian.worklock.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import de.menkalian.worklock.R
import de.menkalian.worklock.ui.navigation.NavigationTargets.*
import java.time.LocalDate

@Composable
fun NavigationView() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, stringResource(R.string.menu_label_tracking)) },
                    label = { Text(stringResource(R.string.menu_label_tracking)) },
                    selected = checkPath(backStackEntry, TRACKING),
                    onClick = {
                        navController.navigate(TRACKING.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(painterResource(R.drawable.icon_edit_calendar), stringResource(R.string.menu_label_calendar)) },
                    label = { Text(stringResource(R.string.menu_label_calendar)) },
                    selected = checkPath(backStackEntry, DAYS) || checkPath(backStackEntry, DAY_CORRECTION),
                    onClick = {
                        navController.navigate(DAYS.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(painterResource(R.drawable.icon_export), stringResource(R.string.menu_label_export)) },
                    label = { Text(stringResource(R.string.menu_label_export)) },
                    selected = checkPath(backStackEntry, EXPORT),
                    onClick = {
                        navController.navigate(EXPORT.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, stringResource(R.string.menu_label_settings)) },
                    label = { Text(stringResource(R.string.menu_label_settings)) },
                    selected = checkPath(backStackEntry, SETTINGS),
                    onClick = {
                        navController.navigate(SETTINGS.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(navController, TRACKING.path, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(TRACKING.path) {
                TrackingView()
            }
            composable(DAYS.path) {
                DaysView(navController)
            }
            composable(
                DAY_CORRECTION.path, listOf(
                    navArgument("year") { defaultValue = 2024 },
                    navArgument("month") { defaultValue = 1 },
                    navArgument("day") { defaultValue = 1 },
                )
            ) { backStackEntry ->
                val y = backStackEntry.arguments?.getInt("year") ?: 2024
                val m = backStackEntry.arguments?.getInt("month") ?: 1
                val d = backStackEntry.arguments?.getInt("day") ?: 1
                val date = LocalDate.of(y, m, d)
                DayRecordCorrectionView(date, navController)
            }
            composable(EXPORT.path) {
                ExportView()
            }
            composable(SETTINGS.path) {
                SettingsView()
            }
        }
    }
}

private fun checkPath(backStackEntry: NavBackStackEntry?, target: NavigationTargets) =
    backStackEntry?.destination?.hierarchy?.any { it.route == target.path } == true

enum class NavigationTargets(val path: String) {
    TRACKING("home"),
    DAYS("days"),
    DAY_CORRECTION("day/{year}/{month}/{day}"),
    EXPORT("export"),
    SETTINGS("settings"),
}
