package com.op1sync.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.op1sync.app.feature.home.HomeScreen
import com.op1sync.app.feature.browser.BrowserScreen
import com.op1sync.app.feature.library.LibraryScreen
import com.op1sync.app.feature.backup.BackupScreen
import com.op1sync.app.feature.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Browser : Screen("browser")
    data object Library : Screen("library")
    data object Backup : Screen("backup")
    data object Settings : Screen("settings")
}

@Composable
fun OP1SyncNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBrowser = { navController.navigate(Screen.Browser.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Browser.route) {
            BrowserScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
