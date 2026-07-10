package com.finemooll.shelka.domain.usecase

import com.finemooll.shelka.domain.model.NewGameDraft
import com.finemooll.shelka.domain.model.Word
import com.finemooll.shelka.domain.repository.GameRepository
import com.finemooll.shelka.domain.repository.WordRepository
import kotlin.random.Random

interface WordShuffler { fun <T> shuffled(items: List<T>): List<T> }
class RandomWordShuffler(private val random: Random = Random.Default) : WordShuffler { override fun <T> shuffled(items: List<T>) = items.shuffled(random) }

sealed interface WordSelectionResult {
    data class Success(val words: List<Word>) : WordSelectionResult
    data class InsufficientWords(val requested: Int, val available: Int) : WordSelectionResult
}

class SelectWordsForNewGameUseCase(private val shuffler: WordShuffler = RandomWordShuffler()) {
    operator fun invoke(requested: Int, difficulty: Int, selectedThemeIds: Set<String>, candidates: List<Word>): WordSelectionResult {
        val eligible = candidates.filter { it.difficulty == difficulty && it.themeId in selectedThemeIds }.distinctBy { it.id }
        if (eligible.size < requested) return WordSelectionResult.InsufficientWords(requested, eligible.size)
        val allThemes = selectedThemeIds.size == NewGameRules.expectedThemeCount
        val perTheme = when { allThemes && requested == 50 -> 2; allThemes && requested == 100 -> 4; else -> null }
        if (perTheme != null) {
            val byTheme = eligible.groupBy { it.themeId }
            if (selectedThemeIds.any { (byTheme[it]?.size ?: 0) < perTheme }) return WordSelectionResult.InsufficientWords(requested, eligible.size)
            return WordSelectionResult.Success(selectedThemeIds.sorted().flatMap { shuffler.shuffled(byTheme.getValue(it)).take(perTheme) })
        }
        return WordSelectionResult.Success(shuffler.shuffled(eligible).take(requested))
    }
}

sealed interface CreateGameResult {
    data class Success(val gameId: String) : CreateGameResult
    data class InvalidDraft(val errors: List<String>) : CreateGameResult
    data class WordConflict(val wordIds: Set<String>) : CreateGameResult
    data class LogoFailure(val message: String, val cause: Throwable? = null) : CreateGameResult
    data class Failure(val cause: Throwable) : CreateGameResult
}

class CreateGameSessionUseCase(private val gameRepository: GameRepository) {
    suspend operator fun invoke(draft: NewGameDraft, selectedWords: List<Word>): CreateGameResult = gameRepository.createGame(draft, selectedWords)
}

class GetAvailableWordsUseCase(private val wordRepository: WordRepository) {
    suspend operator fun invoke(difficulty: Int, themeIds: Set<String>): List<Word> = wordRepository.getAvailableWords(difficulty, themeIds)
}
