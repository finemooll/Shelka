package com.finemooll.shelka.domain.repository

import com.finemooll.shelka.data.importer.ImportResult
import com.finemooll.shelka.domain.model.ThemeSummary
import com.finemooll.shelka.domain.model.Word

interface WordRepository {
    suspend fun ensureWordsImported(): ImportResult
    suspend fun getAllWords(): List<Word>
    suspend fun getThemeSummaries(): List<ThemeSummary>
    suspend fun getWordsByDifficultyAndThemes(difficulty: Int, themeIds: Set<String>): List<Word>
    suspend fun getAvailableWords(difficulty: Int, themeIds: Set<String>): List<Word>
}
