package com.finemooll.shelka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finemooll.shelka.data.local.entity.*

@Dao
interface GameHistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertGameSession(entity: GameSessionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTeam(entity: TeamEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPlayer(entity: PlayerEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSelectedWord(entity: GameSelectedWordEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTurn(entity: TurnEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTurnWordResult(entity: TurnWordResultEntity)

    @Query("SELECT DISTINCT wordId FROM game_selected_words WHERE wordId IN (:wordIds) UNION SELECT DISTINCT wordId FROM turn_word_results WHERE wordId IN (:wordIds)")
    suspend fun getReferencedWordIds(wordIds: Set<String>): List<String>

    @Query("SELECT COUNT(*) FROM game_selected_words WHERE gameId = :gameId AND wordId = :wordId")
    suspend fun selectedWordCount(gameId: String, wordId: String): Int

    @Query("SELECT wordId FROM game_selected_words WHERE wordId IN (:wordIds)")
    suspend fun getAlreadySelectedWordIds(wordIds: Set<String>): List<String>

    @Query("SELECT COUNT(*) FROM game_sessions")
    suspend fun gameSessionCount(): Int

    @Query("SELECT COUNT(*) FROM teams WHERE gameId = :gameId")
    suspend fun teamCount(gameId: String): Int

    @Query("SELECT COUNT(*) FROM players WHERE teamId = :teamId")
    suspend fun playerCount(teamId: String): Int

    @Query("SELECT COUNT(*) FROM game_selected_words WHERE gameId = :gameId")
    suspend fun selectedWordCountForGame(gameId: String): Int

    @Query("SELECT * FROM game_sessions WHERE id = :gameId")
    suspend fun getGameSession(gameId: String): GameSessionEntity?

    @Query("SELECT * FROM teams WHERE gameId = :gameId ORDER BY orderIndex")
    suspend fun getTeamsForGame(gameId: String): List<TeamEntity>

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY orderIndex")
    suspend fun getPlayersForTeam(teamId: String): List<PlayerEntity>

    @Query("SELECT * FROM game_selected_words WHERE gameId = :gameId ORDER BY selectedOrder")
    suspend fun getSelectedWordsForGame(gameId: String): List<GameSelectedWordEntity>

    @Query("SELECT COUNT(*) FROM turn_word_results WHERE turnId = :turnId AND wordId = :wordId")
    suspend fun turnWordResultCount(turnId: String, wordId: String): Int
}
