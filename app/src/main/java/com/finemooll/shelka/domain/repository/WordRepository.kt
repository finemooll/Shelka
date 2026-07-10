package com.finemooll.shelka.domain.repository

import com.finemooll.shelka.data.json.WordValidationError
import com.finemooll.shelka.domain.model.ThemeSummary
import com.finemooll.shelka.domain.model.Word

sealed interface ImportResult {
    data object Imported : ImportResult
    data object AlreadyImported : ImportResult
    data class ValidationFailed(val errors: List<WordValidationError>) : ImportResult
    data class Failed(val cause: Throwable) : ImportResult
}

interface WordRepository {
    suspend fun ensureWordsImported(): ImportResult
    suspend fun getAllWords(): List<Word>
    suspend fun getThemeSummaries(): List<ThemeSummary>
    suspend fun getWordsByDifficultyAndThemes(difficulty: Int, themeIds: Set<String>): List<Word>

    /**
     * PR2 has no completed-game filtering yet. Future PRs must exclude used words via
     * GameSelectedWordEntity's gameId + wordId relationship, never via mutable Word state.
     */
    suspend fun getAvailableWords(difficulty: Int, themeIds: Set<String>): List<Word>
}
