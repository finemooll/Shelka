package com.finemooll.shelka.data

import com.finemooll.shelka.domain.model.*
import com.finemooll.shelka.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test

class NewGameDomainTest {
    private fun team(id: String = "t", p1: String = "A", p2: String = "B") = TeamDraft(id, "Team $id", PlayerDraft("$id-p1", p1), PlayerDraft("$id-p2", p2), null)
    private val themes = (1..25).map { Theme("theme$it", if (it == 24) "Интим 18+" else if (it == 25) "Жёсткий 18+" else "Theme $it") }
    private fun words() = themes.flatMap { theme -> (1..4).map { Word("${theme.id}-$it", "w", theme.id, theme.name, 1) } }

    @Test fun teamValidationCoversCountsAndFields() {
        val useCase = ValidateTeamsUseCase()
        assertFalse(useCase(listOf(team())).isValid)
        assertFalse(useCase((1..9).map { team("t$it") }).isValid)
        assertTrue(useCase(listOf(team("a"), team("b"))).isValid)
        assertTrue(useCase((1..8).map { team("t$it") }).isValid)
        assertFalse(useCase(listOf(team("a").copy(name = " "), team("b"))).isValid)
        assertFalse(useCase(listOf(team("a", p1 = " "), team("b"))).isValid)
        assertFalse(useCase(listOf(team("a", p2 = " "), team("b"))).isValid)
        assertFalse(useCase(listOf(team("a", p1 = "Alex", p2 = " alex "), team("b"))).isValid)
        assertFalse(useCase(listOf(team("a"), team("a"))).isValid)
    }

    @Test fun settingsValidationCoversAllowedValuesAndThemes() {
        val useCase = ValidateGameSettingsUseCase()
        NewGameRules.allowedWordCounts.forEach { assertTrue(useCase(GameSettingsDraft(it, 1, 60, setOf("theme1")), themes).isValid) }
        assertFalse(useCase(GameSettingsDraft(25, 1, 60, setOf("theme1")), themes).isValid)
        listOf(1, 2, 3).forEach { assertTrue(useCase(GameSettingsDraft(20, it, 60, setOf("theme1")), themes).isValid) }
        assertFalse(useCase(GameSettingsDraft(20, 4, 60, setOf("theme1")), themes).isValid)
        assertTrue(useCase(GameSettingsDraft(20, 1, 20, setOf("theme1")), themes).isValid)
        assertTrue(useCase(GameSettingsDraft(20, 1, 180, setOf("theme1")), themes).isValid)
        assertFalse(useCase(GameSettingsDraft(20, 1, 10, setOf("theme1")), themes).isValid)
        assertFalse(useCase(GameSettingsDraft(20, 1, 190, setOf("theme1")), themes).isValid)
        assertFalse(useCase(GameSettingsDraft(20, 1, 25, setOf("theme1")), themes).isValid)
        assertFalse(useCase(GameSettingsDraft(20, 1, 60, emptySet()), themes).isValid)
        assertFalse(useCase(GameSettingsDraft(20, 1, 60, setOf("missing")), themes).isValid)
    }

    @Test fun wordSelectionUsesFiltersDistributionAndInsufficientResult() {
        val useCase = SelectWordsForNewGameUseCase(object : WordShuffler { override fun <T> shuffled(items: List<T>) = items })
        val allThemeIds = themes.map { it.id }.toSet()
        val fifty = useCase(50, 1, allThemeIds, words()) as WordSelectionResult.Success
        assertEquals(50, fifty.words.size)
        assertTrue(fifty.words.all { it.difficulty == 1 && it.themeId in allThemeIds })
        assertEquals(50, fifty.words.map { it.id }.toSet().size)
        assertTrue(fifty.words.groupingBy { it.themeId }.eachCount().values.all { it == 2 })
        val hundred = useCase(100, 1, allThemeIds, words()) as WordSelectionResult.Success
        assertTrue(hundred.words.groupingBy { it.themeId }.eachCount().values.all { it == 4 })
        val subset = setOf("theme1", "theme2", "theme3")
        val selected = useCase(5, 1, subset, words()) as WordSelectionResult.Success
        assertEquals(5, selected.words.size)
        assertTrue(selected.words.all { it.themeId in subset })
        assertTrue(useCase(101, 1, allThemeIds, words()) is WordSelectionResult.InsufficientWords)
    }
}
