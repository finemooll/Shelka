package com.finemooll.shelka.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ShelkaLightColorScheme = lightColorScheme(
    primary = Walnut,
    onPrimary = Cream,
    secondary = WarmBeige,
    onSecondary = DarkCocoa,
    background = Cream,
    onBackground = DarkCocoa,
    surface = Cream,
    onSurface = DarkCocoa,
)

@Composable
fun ShelkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShelkaLightColorScheme,
        content = content,
    )
}
