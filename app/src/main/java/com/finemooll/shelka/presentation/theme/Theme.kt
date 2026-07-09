package com.finemooll.shelka.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ShelkaLightColorScheme = lightColorScheme(
    primary = DeepBrown,
    onPrimary = Cream,
    secondary = SoftBrown,
    background = Cream,
    onBackground = DeepBrown,
    surface = Cream,
    onSurface = DeepBrown,
)

@Composable
fun ShelkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShelkaLightColorScheme,
        typography = ShelkaTypography,
        content = content,
    )
}
