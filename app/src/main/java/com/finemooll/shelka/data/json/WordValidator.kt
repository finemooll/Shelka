package com.finemooll.shelka.data.json

private const val ExpectedWordCount = 2025
private const val ExpectedThemeCount = 25
private const val ExpectedWordsPerThemeDifficulty = 27
private val AllowedDifficulties = setOf(1, 2, 3)

sealed interface WordValidationError {
    data class BlankField(val index: Int, val fieldName: String) : WordValidationError
    data class InvalidDifficulty(val index: Int, val id: String, val difficulty: Int) : WordValidationError
    data class DuplicateId(val id: String, val firstIndex: Int, val duplicateIndex: Int) : WordValidationError
    data class UnexpectedWordCount(val expected: Int, val actual: Int) : WordValidationError
    data class UnexpectedThemeCount(val expected: Int, val actual: Int) : WordValidationError
    data class UnexpectedThemeDifficultyCount(
        val themeId: String,
        val difficulty: Int,
        val expected: Int,
        val actual: Int,
    ) : WordValidationError
    data class UnstableThemeName(val themeId: String, val themeNames: Set<String>) : WordValidationError
}

sealed interface WordValidationResult {
    data object Valid : WordValidationResult
    data class Invalid(val errors: List<WordValidationError>) : WordValidationResult
}

interface WordValidator {
    fun validate(words: List<WordDto>): WordValidationResult
}

class DefaultWordValidator : WordValidator {
    override fun validate(words: List<WordDto>): WordValidationResult {
        val errors = mutableListOf<WordValidationError>()
        words.forEachIndexed { index, word ->
            if (word.id.isBlank()) errors += WordValidationError.BlankField(index, "id")
            if (word.text.isBlank()) errors += WordValidationError.BlankField(index, "text")
            if (word.themeId.isBlank()) errors += WordValidationError.BlankField(index, "themeId")
            if (word.themeName.isBlank()) errors += WordValidationError.BlankField(index, "themeName")
            if (word.difficulty !in AllowedDifficulties) errors += WordValidationError.InvalidDifficulty(index, word.id, word.difficulty)
        }
        val firstIndexes = mutableMapOf<String, Int>()
        words.forEachIndexed { index, word ->
            val previous = firstIndexes.putIfAbsent(word.id, index)
            if (previous != null) errors += WordValidationError.DuplicateId(word.id, previous, index)
        }
        if (words.size != ExpectedWordCount) errors += WordValidationError.UnexpectedWordCount(ExpectedWordCount, words.size)
        val themeIds = words.map { it.themeId }.toSet()
        if (themeIds.size != ExpectedThemeCount) errors += WordValidationError.UnexpectedThemeCount(ExpectedThemeCount, themeIds.size)
        themeIds.forEach { themeId ->
            AllowedDifficulties.forEach { difficulty ->
                val count = words.count { it.themeId == themeId && it.difficulty == difficulty }
                if (count != ExpectedWordsPerThemeDifficulty) {
                    errors += WordValidationError.UnexpectedThemeDifficultyCount(themeId, difficulty, ExpectedWordsPerThemeDifficulty, count)
                }
            }
        }
        words.groupBy { it.themeId }.forEach { (themeId, themeWords) ->
            val names = themeWords.map { it.themeName }.toSet()
            if (names.size > 1) errors += WordValidationError.UnstableThemeName(themeId, names)
        }
        return if (errors.isEmpty()) WordValidationResult.Valid else WordValidationResult.Invalid(errors)
    }
}
