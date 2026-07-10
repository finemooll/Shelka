package com.finemooll.shelka.data.json

sealed interface WordValidationResult { data object Valid: WordValidationResult; data class Invalid(val errors: List<WordValidationError>): WordValidationResult }
sealed interface WordValidationError {
    data class BlankField(val index: Int, val field: String): WordValidationError
    data class InvalidDifficulty(val index: Int, val id: String, val difficulty: Int): WordValidationError
    data class DuplicateId(val id: String): WordValidationError
    data class InvalidWordCount(val expected: Int, val actual: Int): WordValidationError
    data class InvalidThemeCount(val expected: Int, val actual: Int): WordValidationError
    data class InvalidThemeDifficultyCount(val themeId: String, val difficulty: Int, val expected: Int, val actual: Int): WordValidationError
    data class UnstableThemeName(val themeId: String, val names: Set<String>): WordValidationError
}

interface WordValidator { fun validate(words: List<WordDto>): WordValidationResult }

class DefaultWordValidator : WordValidator {
    override fun validate(words: List<WordDto>): WordValidationResult {
        val errors = mutableListOf<WordValidationError>()
        words.forEachIndexed { i, w ->
            if (w.id.isBlank()) errors += WordValidationError.BlankField(i, "id")
            if (w.text.isBlank()) errors += WordValidationError.BlankField(i, "text")
            if (w.themeId.isBlank()) errors += WordValidationError.BlankField(i, "themeId")
            if (w.themeName.isBlank()) errors += WordValidationError.BlankField(i, "themeName")
            if (w.difficulty !in 1..3) errors += WordValidationError.InvalidDifficulty(i, w.id, w.difficulty)
        }
        words.groupBy { it.id }.filter { it.key.isNotBlank() && it.value.size > 1 }.keys.forEach { errors += WordValidationError.DuplicateId(it) }
        if (words.size != 2025) errors += WordValidationError.InvalidWordCount(2025, words.size)
        val themeIds = words.map { it.themeId }.filter { it.isNotBlank() }.toSet()
        if (themeIds.size != 25) errors += WordValidationError.InvalidThemeCount(25, themeIds.size)
        themeIds.forEach { themeId -> (1..3).forEach { difficulty ->
            val actual = words.count { it.themeId == themeId && it.difficulty == difficulty }
            if (actual != 27) errors += WordValidationError.InvalidThemeDifficultyCount(themeId, difficulty, 27, actual)
        } }
        words.groupBy { it.themeId }.filterKeys { it.isNotBlank() }.forEach { (themeId, group) ->
            val names = group.map { it.themeName }.filter { it.isNotBlank() }.toSet()
            if (names.size != 1) errors += WordValidationError.UnstableThemeName(themeId, names)
        }
        return if (errors.isEmpty()) WordValidationResult.Valid else WordValidationResult.Invalid(errors)
    }
}
