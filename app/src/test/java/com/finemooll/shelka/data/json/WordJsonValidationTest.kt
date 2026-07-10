package com.finemooll.shelka.data.json

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class WordJsonValidationTest {
    private val parser = KotlinxWordJsonParser()
    private val validator = DefaultWordValidator()
    private fun realWords() = parser.parse(File("src/main/assets/words.json").readText())

    @Test fun realWordsJsonParsesSuccessfully() { assertEquals(2025, realWords().size) }
    @Test fun realFileHasUniqueIdsValidDifficultiesAndThemeShape() {
        val words = realWords()
        assertEquals(2025, words.size)
        assertEquals(words.size, words.map { it.id }.toSet().size)
        assertTrue(words.all { it.difficulty in 1..3 })
        assertEquals(25, words.map { it.themeId }.toSet().size)
        words.groupBy { it.themeId }.forEach { (_, themeWords) ->
            assertEquals(1, themeWords.map { it.themeName }.toSet().size)
            (1..3).forEach { difficulty -> assertEquals(27, themeWords.count { it.difficulty == difficulty }) }
        }
    }
    @Test fun validatorSucceedsForRealFile() { assertSame(WordValidationResult.Valid, validator.validate(realWords())) }
    @Test fun validatorFailsForDuplicateIds() {
        val words = realWords().toMutableList(); words[1] = words[1].copy(id = words[0].id)
        val result = validator.validate(words)
        assertTrue((result as WordValidationResult.Invalid).errors.any { it is WordValidationError.DuplicateId })
    }
    @Test fun validatorFailsForBlankText() {
        val words = realWords().toMutableList(); words[0] = words[0].copy(text = " ")
        val result = validator.validate(words)
        assertTrue((result as WordValidationResult.Invalid).errors.any { it is WordValidationError.BlankField && it.fieldName == "text" })
    }
    @Test fun validatorFailsForInvalidDifficulty() {
        val words = realWords().toMutableList(); words[0] = words[0].copy(difficulty = 4)
        val result = validator.validate(words)
        assertTrue((result as WordValidationResult.Invalid).errors.any { it is WordValidationError.InvalidDifficulty })
    }
    @Test fun parserRejectsNonArrayRoot() { assertThrows(WordJsonParseException::class.java) { parser.parse("{}") } }
}
