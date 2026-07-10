package com.finemooll.shelka.data.json

import kotlinx.serialization.Serializable

@Serializable
data class WordDto(
    val id: String,
    val text: String,
    val themeId: String,
    val themeName: String,
    val difficulty: Int,
)
