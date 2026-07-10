package com.finemooll.shelka.data.repository

import androidx.room.withTransaction
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.local.entity.*
import com.finemooll.shelka.data.logo.LogoStorageResult
import com.finemooll.shelka.data.logo.TeamLogoStorage
import com.finemooll.shelka.domain.model.NewGameDraft
import com.finemooll.shelka.domain.model.TeamDraft
import com.finemooll.shelka.domain.model.Word
import com.finemooll.shelka.domain.repository.GameRepository
import com.finemooll.shelka.domain.usecase.CreateGameResult
import com.finemooll.shelka.domain.usecase.NewGameRules
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.UUID

class RoomGameRepository(
    private val database: AppDatabase,
    private val logoStorage: TeamLogoStorage,
) : GameRepository {
    override suspend fun createGame(draft: NewGameDraft, selectedWords: List<Word>): CreateGameResult {
        val errors = validateCreationInput(draft, selectedWords)
        if (errors.isNotEmpty()) return CreateGameResult.InvalidDraft(errors)
        val gameId = UUID.randomUUID().toString()
        val adoptedLogos = mutableListOf<String>()
        return try {
            val teamsWithStableLogos = mutableListOf<TeamDraft>()
            for (team in draft.teams) {
                when (val logo = logoStorage.adoptDraftLogo(team.logoPath, gameId, team.id)) {
                    is LogoStorageResult.Success -> {
                        val stable = team.copy(logoPath = logo.internalPath.ifBlank { null })
                        stable.logoPath?.let(adoptedLogos::add)
                        teamsWithStableLogos += stable
                    }
                    is LogoStorageResult.Failure -> {
                        logoStorage.deleteAdoptedLogos(adoptedLogos)
                        return CreateGameResult.InvalidDraft(listOf(logo.message))
                    }
                }
            }
            val now = System.currentTimeMillis()
            database.withTransaction {
                val dao = database.gameHistoryDao()
                val conflicts = dao.getAlreadySelectedWordIds(selectedWords.map { it.id }.toSet()).toSet()
                if (conflicts.isNotEmpty()) throw WordAvailabilityConflictException(conflicts)
                dao.insertGameSession(GameSessionEntity(gameId, now, now, null, GameSessionStatus.IN_PROGRESS, draft.settings.wordCount, draft.settings.difficulty, draft.settings.timerSeconds, 0))
                teamsWithStableLogos.forEachIndexed { teamIndex, team ->
                    dao.insertTeam(TeamEntity(team.id, gameId, team.name.trim(), team.logoPath, teamIndex))
                    dao.insertPlayer(PlayerEntity(team.player1.id, team.id, team.player1.name.trim(), 0))
                    dao.insertPlayer(PlayerEntity(team.player2.id, team.id, team.player2.name.trim(), 1))
                }
                selectedWords.forEachIndexed { index, word -> dao.insertSelectedWord(GameSelectedWordEntity(gameId, word.id, index)) }
            }
            CreateGameResult.Success(gameId)
        } catch (conflict: WordAvailabilityConflictException) {
            logoStorage.deleteAdoptedLogos(adoptedLogos)
            CreateGameResult.WordConflict(conflict.wordIds)
        } catch (cancel: CancellationException) {
            logoStorage.deleteAdoptedLogos(adoptedLogos)
            throw cancel
        } catch (t: Throwable) {
            logoStorage.deleteAdoptedLogos(adoptedLogos)
            CreateGameResult.Failure(t)
        }
    }

    private fun validateCreationInput(draft: NewGameDraft, selectedWords: List<Word>): List<String> {
        val errors = mutableListOf<String>()
        if (draft.teams.size !in NewGameRules.minTeams..NewGameRules.maxTeams) errors += "Team count must be 2..8"
        val ids = draft.teams.flatMap { listOf(it.id, it.player1.id, it.player2.id) }
        if (ids.any { it.isBlank() } || ids.toSet().size != ids.size) errors += "Team and player IDs must be non-blank and unique"
        draft.teams.forEach { team -> validateTeam(team, errors) }
        val settings = draft.settings
        if (settings.wordCount !in NewGameRules.allowedWordCounts) errors += "Unsupported word count"
        if (settings.difficulty !in NewGameRules.allowedDifficulties) errors += "Unsupported difficulty"
        if (settings.timerSeconds !in NewGameRules.minTimerSeconds..NewGameRules.maxTimerSeconds || settings.timerSeconds % NewGameRules.timerStepSeconds != 0) errors += "Unsupported timer"
        if (settings.selectedThemeIds.isEmpty()) errors += "At least one theme must be selected"
        if (selectedWords.size != settings.wordCount) errors += "Selected word count mismatch"
        if (selectedWords.map { it.id }.toSet().size != selectedWords.size) errors += "Selected word IDs must be unique"
        if (selectedWords.any { it.difficulty != settings.difficulty }) errors += "Selected words must match requested difficulty"
        if (selectedWords.any { it.themeId !in settings.selectedThemeIds }) errors += "Selected words must belong to selected themes"
        return errors
    }

    private fun validateTeam(team: TeamDraft, errors: MutableList<String>) {
        val p1 = team.player1.name.trim()
        val p2 = team.player2.name.trim()
        if (team.name.isBlank()) errors += "Team name must not be blank"
        if (p1.isBlank() || p2.isBlank()) errors += "Player names must not be blank"
        if (p1.isNotBlank() && p1.lowercase(Locale.getDefault()) == p2.lowercase(Locale.getDefault())) errors += "Players within a team must differ"
    }

    private class WordAvailabilityConflictException(val wordIds: Set<String>) : RuntimeException()
}
