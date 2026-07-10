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

    @Query("SELECT COUNT(*) FROM turn_word_results WHERE turnId = :turnId AND wordId = :wordId")
    suspend fun turnWordResultCount(turnId: String, wordId: String): Int
}
