package com.finemooll.shelka.domain.model

data class Word(
    val id: String,
    val text: String,
    val themeId: String,
    val themeName: String,
    val difficulty: Int,
)
