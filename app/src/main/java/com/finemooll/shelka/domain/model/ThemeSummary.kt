package com.finemooll.shelka.domain.model

data class ThemeSummary(
    val themeId: String,
    val themeName: String,
    val count: Int,
    val countByDifficulty: Map<Int, Int>,
)
