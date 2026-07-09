package com.finemooll.shelka.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NewGameScreen(onBackClick: () -> Unit) {
    PlaceholderScreen(title = "Новая игра", onBackClick = onBackClick)
}

@Composable
fun HistoryScreen(onBackClick: () -> Unit) {
    PlaceholderScreen(title = "История", onBackClick = onBackClick)
}

@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    PlaceholderScreen(title = "Настройки", onBackClick = onBackClick)
}

@Composable
private fun PlaceholderScreen(
    title: String,
    onBackClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(
                modifier = Modifier.padding(top = 32.dp),
                onClick = onBackClick,
            ) {
                Text(text = "Назад")
            }
        }
    }
}
