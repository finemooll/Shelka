package com.finemooll.shelka.data.repository

import androidx.room.withTransaction
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.local.entity.*
import com.finemooll.shelka.domain.model.NewGameDraft
import com.finemooll.shelka.domain.model.Word
import com.finemooll.shelka.domain.repository.GameRepository
import com.finemooll.shelka.domain.usecase.CreateGameResult
import java.util.UUID

class RoomGameRepository(private val database: AppDatabase) : GameRepository {
    override suspend fun createGame(draft: NewGameDraft, selectedWords: List<Word>): CreateGameResult = try {
        require(selectedWords.size == draft.settings.wordCount) { "Selected word count mismatch" }
        val gameId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.withTransaction {
            val dao = database.gameHistoryDao()
            dao.insertGameSession(GameSessionEntity(gameId, now, now, null, GameSessionStatus.IN_PROGRESS, draft.settings.wordCount, draft.settings.difficulty, draft.settings.timerSeconds, 0))
            draft.teams.forEachIndexed { teamIndex, team ->
                dao.insertTeam(TeamEntity(team.id, gameId, team.name.trim(), team.logoPath, teamIndex))
                dao.insertPlayer(PlayerEntity(team.player1.id, team.id, team.player1.name.trim(), 0))
                dao.insertPlayer(PlayerEntity(team.player2.id, team.id, team.player2.name.trim(), 1))
            }
            selectedWords.forEachIndexed { index, word -> dao.insertSelectedWord(GameSelectedWordEntity(gameId, word.id, index)) }
        }
        CreateGameResult.Success(gameId)
    } catch (t: Throwable) { CreateGameResult.Failure(t) }
}
