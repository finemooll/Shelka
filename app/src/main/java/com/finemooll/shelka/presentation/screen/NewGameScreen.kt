package com.finemooll.shelka.presentation.screen

import androidx.compose.runtime.Composable

@Composable
fun NewGameScreen(onBackClick: () -> Unit) {
    PlaceholderScreen(
        title = "Новая игра",
        onBackClick = onBackClick,
    )
}
