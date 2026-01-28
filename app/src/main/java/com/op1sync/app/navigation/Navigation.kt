package com.op1sync.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.op1sync.app.data.local.FileType
import com.op1sync.app.feature.home.HomeScreen
import com.op1sync.app.feature.browser.BrowserScreen
import com.op1sync.app.feature.library.LibraryScreen
import com.op1sync.app.feature.library.TapesScreen
import com.op1sync.app.feature.library.SynthScreen
import com.op1sync.app.feature.library.DrumScreen
import com.op1sync.app.feature.library.DrumKeyboardScreen
import com.op1sync.app.feature.library.MixdownScreen
import com.op1sync.app.feature.backup.BackupScreen
import com.op1sync.app.feature.settings.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Browser : Screen("browser")
    data object Library : Screen("library")
    data object Tapes : Screen("library/tapes")
    data object Synth : Screen("library/synth")
    data object Drum : Screen("library/drum")
    data object DrumKeyboard : Screen("library/drum/keyboard/{filePath}") {
        fun createRoute(filePath: String): String {
            val encoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
            return "library/drum/keyboard/$encoded"
        }
    }
    data object Mixdown : Screen("library/mixdown")
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
                onNavigateToTapes = { navController.navigate(Screen.Tapes.route) },
                onNavigateToSynth = { navController.navigate(Screen.Synth.route) },
                onNavigateToDrum = { navController.navigate(Screen.Drum.route) },
                onNavigateToMixdown = { navController.navigate(Screen.Mixdown.route) },
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
        composable(Screen.Tapes.route) {
            TapesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Synth.route) {
            SynthScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Drum.route) {
            DrumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDrumKeyboard = { filePath ->
                    navController.navigate(Screen.DrumKeyboard.createRoute(filePath))
                }
            )
        }
        composable(
            route = Screen.DrumKeyboard.route,
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val filePath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            DrumKeyboardScreen(
                filePath = filePath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Mixdown.route) {
            MixdownScreen(
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
