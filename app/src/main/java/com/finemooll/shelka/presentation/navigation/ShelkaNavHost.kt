package com.finemooll.shelka.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.finemooll.shelka.presentation.screen.HistoryScreen
import com.finemooll.shelka.presentation.screen.MainMenuScreen
import com.finemooll.shelka.presentation.screen.NewGameScreen
import com.finemooll.shelka.presentation.screen.SettingsScreen

@Composable
fun ShelkaNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ShelkaRoute.MainMenu.route,
    ) {
        composable(ShelkaRoute.MainMenu.route) {
            MainMenuScreen(
                onNewGameClick = { navController.navigate(ShelkaRoute.NewGame.route) },
                onHistoryClick = { navController.navigate(ShelkaRoute.History.route) },
                onSettingsClick = { navController.navigate(ShelkaRoute.Settings.route) },
            )
        }
        composable(ShelkaRoute.NewGame.route) {
            NewGameScreen(onBackClick = navController::popBackStack)
        }
        composable(ShelkaRoute.History.route) {
            HistoryScreen(onBackClick = navController::popBackStack)
        }
        composable(ShelkaRoute.Settings.route) {
            SettingsScreen(onBackClick = navController::popBackStack)
        }
    }
}
