package com.finemooll.shelka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finemooll.shelka.data.local.entity.WordEntity

data class ThemeSummaryRow(val themeId: String, val themeName: String, val difficulty: Int, val count: Int)

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Update
    suspend fun updateAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT * FROM words ORDER BY id")
    suspend fun getAll(): List<WordEntity>

    @Query("SELECT id FROM words ORDER BY id")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM words WHERE difficulty = :difficulty ORDER BY id")
    suspend fun getByDifficulty(difficulty: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE difficulty = :difficulty AND themeId IN (:themeIds) ORDER BY id")
    suspend fun getByDifficultyAndThemeIds(difficulty: Int, themeIds: Set<String>): List<WordEntity>

    @Query("SELECT themeId, themeName, difficulty, COUNT(*) AS count FROM words GROUP BY themeId, themeName, difficulty ORDER BY themeName, difficulty")
    suspend fun getThemeSummaryRows(): List<ThemeSummaryRow>

    @Query("DELETE FROM words WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Set<String>)

    @Query("DELETE FROM words")
    suspend fun clearForTest()
}
