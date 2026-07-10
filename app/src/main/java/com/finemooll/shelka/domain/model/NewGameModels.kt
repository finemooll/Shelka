package com.finemooll.shelka.domain.model

data class PlayerDraft(val id: String, val name: String)

data class TeamDraft(
    val id: String,
    val name: String,
    val player1: PlayerDraft,
    val player2: PlayerDraft,
    val logoPath: String?,
)

data class GameSettingsDraft(
    val wordCount: Int = 50,
    val difficulty: Int = 1,
    val timerSeconds: Int = 60,
    val selectedThemeIds: Set<String> = emptySet(),
)

data class NewGameDraft(val teams: List<TeamDraft>, val settings: GameSettingsDraft)

data class Theme(val id: String, val name: String)
