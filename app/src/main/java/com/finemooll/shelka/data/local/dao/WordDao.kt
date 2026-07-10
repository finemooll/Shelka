package com.finemooll.shelka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finemooll.shelka.data.local.entity.WordEntity

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT * FROM words ORDER BY id")
    suspend fun getAll(): List<WordEntity>

    @Query("SELECT * FROM words WHERE difficulty = :difficulty ORDER BY id")
    suspend fun getByDifficulty(difficulty: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE difficulty = :difficulty AND themeId IN (:themeIds) ORDER BY id")
    suspend fun getByDifficultyAndThemeIds(difficulty: Int, themeIds: Set<String>): List<WordEntity>

    @Query("SELECT themeId, themeName, difficulty, COUNT(*) AS count FROM words GROUP BY themeId, themeName, difficulty ORDER BY themeName, difficulty")
    suspend fun getThemeSummaryRows(): List<ThemeSummaryRow>

    @Query("DELETE FROM words")
    suspend fun clear()
}

data class ThemeSummaryRow(
    val themeId: String,
    val themeName: String,
    val difficulty: Int,
    val count: Int,
)
