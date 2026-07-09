package com.finemooll.shelka.presentation.navigation

sealed class ShelkaRoute(val route: String) {
    data object MainMenu : ShelkaRoute("main_menu")
    data object NewGame : ShelkaRoute("new_game")
    data object History : ShelkaRoute("history")
    data object Settings : ShelkaRoute("settings")
}
