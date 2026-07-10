package com.finemooll.shelka.domain.usecase

import com.finemooll.shelka.domain.model.GameSettingsDraft
import com.finemooll.shelka.domain.model.TeamDraft
import com.finemooll.shelka.domain.model.Theme
import java.util.Locale

object NewGameRules {
    val allowedWordCounts = setOf(20, 30, 40, 50, 60, 70, 80, 90, 100)
    val allowedDifficulties = setOf(1, 2, 3)
    const val minTimerSeconds = 20
    const val maxTimerSeconds = 180
    const val timerStepSeconds = 10
    const val minTeams = 2
    const val maxTeams = 8
    const val expectedThemeCount = 25
    const val insufficientWordsMessage = "Недостаточно новых слов для выбранных настроек. Очистите историю или выберите меньше слов/другие темы."
}

data class TeamFieldErrors(
    val teamName: String? = null,
    val player1Name: String? = null,
    val player2Name: String? = null,
    val duplicatePlayers: String? = null,
    val idError: String? = null,
)

data class TeamValidationErrors(
    val teamCount: String? = null,
    val duplicateIds: String? = null,
    val perTeam: Map<String, TeamFieldErrors> = emptyMap(),
) { val isValid: Boolean get() = teamCount == null && duplicateIds == null && perTeam.values.all { it == TeamFieldErrors() } }

class ValidateTeamsUseCase {
    operator fun invoke(teams: List<TeamDraft>): TeamValidationErrors {
        val countError = when {
            teams.size < NewGameRules.minTeams -> "Нужно минимум 2 команды"
            teams.size > NewGameRules.maxTeams -> "Можно добавить не больше 8 команд"
            else -> null
        }
        val ids = teams.flatMap { listOf(it.id, it.player1.id, it.player2.id) }
        val duplicateIds = if (ids.any { it.isBlank() } || ids.toSet().size != ids.size) "ID должны быть непустыми и уникальными" else null
        val per = teams.associate { team ->
            val p1 = team.player1.name.trim()
            val p2 = team.player2.name.trim()
            team.id to TeamFieldErrors(
                teamName = if (team.name.isBlank()) "Введите название команды" else null,
                player1Name = if (p1.isBlank()) "Введите имя игрока 1" else null,
                player2Name = if (p2.isBlank()) "Введите имя игрока 2" else null,
                duplicatePlayers = if (p1.isNotBlank() && p1.lowercase(Locale.getDefault()) == p2.lowercase(Locale.getDefault())) "Имена игроков в команде должны отличаться" else null,
                idError = if (team.id.isBlank() || team.player1.id.isBlank() || team.player2.id.isBlank()) "ID не должен быть пустым" else null,
            )
        }
        return TeamValidationErrors(countError, duplicateIds, per)
    }
}

data class SettingsValidationErrors(
    val wordCount: String? = null,
    val difficulty: String? = null,
    val timerSeconds: String? = null,
    val selectedThemes: String? = null,
) { val isValid: Boolean get() = listOf(wordCount, difficulty, timerSeconds, selectedThemes).all { it == null } }

class ValidateGameSettingsUseCase {
    operator fun invoke(settings: GameSettingsDraft, availableThemes: List<Theme>): SettingsValidationErrors {
        val known = availableThemes.map { it.id }.toSet()
        return SettingsValidationErrors(
            wordCount = if (settings.wordCount in NewGameRules.allowedWordCounts) null else "Выберите допустимое количество слов",
            difficulty = if (settings.difficulty in NewGameRules.allowedDifficulties) null else "Выберите сложность 1, 2 или 3",
            timerSeconds = if (settings.timerSeconds in NewGameRules.minTimerSeconds..NewGameRules.maxTimerSeconds && settings.timerSeconds % NewGameRules.timerStepSeconds == 0) null else "Таймер должен быть от 20 до 180 секунд с шагом 10",
            selectedThemes = when {
                settings.selectedThemeIds.isEmpty() -> "Выберите хотя бы одну тему"
                !known.containsAll(settings.selectedThemeIds) -> "Выбрана неизвестная тема"
                else -> null
            },
        )
    }
}
