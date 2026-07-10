package com.finemooll.shelka.data

import android.net.Uri
import com.finemooll.shelka.app.AppInitializationState
import com.finemooll.shelka.data.logo.LogoStorageResult
import com.finemooll.shelka.data.logo.TeamLogoStorage
import com.finemooll.shelka.domain.model.*
import com.finemooll.shelka.domain.repository.GameRepository
import com.finemooll.shelka.domain.repository.WordRepository
import com.finemooll.shelka.domain.usecase.*
import com.finemooll.shelka.presentation.viewmodel.NewGameStatus
import com.finemooll.shelka.presentation.viewmodel.NewGameViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*

class NewGameViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @Before fun setMain() { Dispatchers.setMain(dispatcher) }
    @After fun resetMain() { Dispatchers.resetMain() }

    @Test fun initialStateLoadsAllThemesAndBlocksInvalidTeams() = runTest {
        val vm = vm()
        val state = vm.uiState.value
        Assert.assertEquals(2, state.teams.size)
        Assert.assertEquals(60, state.timerSeconds)
        Assert.assertEquals(25, state.selectedThemeIds.size)
        Assert.assertTrue(state.availableThemes.any { it.name == "Интим 18+" && it.id in state.selectedThemeIds })
        Assert.assertTrue(state.availableThemes.any { it.name == "Жёсткий 18+" && it.id in state.selectedThemeIds })
        Assert.assertFalse(vm.continueToSettings())
    }

    @Test fun duplicateStartsDoNotCreateDuplicateGamesAndSuccessExposesId() = runTest {
        val gameRepo = FakeGameRepository()
        val vm = vm(gameRepository = gameRepo)
        makeValid(vm)
        Assert.assertTrue(vm.continueToSettings())
        vm.startGame(); vm.startGame()
        Assert.assertEquals(1, gameRepo.calls)
        Assert.assertEquals("game-1", vm.uiState.value.createdGameId)
    }

    @Test fun initializationLoadingOrErrorBlocksCreationAndInsufficientWordsShowsExactMessage() = runTest {
        val init = MutableStateFlow<AppInitializationState>(AppInitializationState.Loading)
        val wordRepo = FakeWordRepository(emptyList())
        val vm = vm(wordRepository = wordRepo, initialization = init)
        makeValid(vm)
        vm.continueToSettings(); vm.startGame()
        Assert.assertNull(vm.uiState.value.createdGameId)
        init.value = AppInitializationState.Ready
        vm.startGame()
        Assert.assertEquals(NewGameRules.insufficientWordsMessage, vm.uiState.value.fatalError)
    }

    @Test fun creationFailureClearsLoadingAndCancellationPropagates() = runTest {
        val failing = object : GameRepository { override suspend fun createGame(draft: NewGameDraft, selectedWords: List<Word>) = CreateGameResult.Failure(IllegalStateException()) }
        val vm = vm(gameRepository = failing)
        makeValid(vm); vm.continueToSettings(); vm.startGame()
        Assert.assertFalse(vm.uiState.value.isLoading)
        Assert.assertTrue(vm.uiState.value.status is NewGameStatus.Error)

        val cancelling = object : GameRepository { override suspend fun createGame(draft: NewGameDraft, selectedWords: List<Word>): CreateGameResult { throw CancellationException("cancel") } }
        val cancelledVm = vm(gameRepository = cancelling)
        makeValid(cancelledVm); cancelledVm.continueToSettings()
        cancelledVm.startGame()
        Assert.assertFalse(cancelledVm.uiState.value.status is NewGameStatus.Error)
    }

    private fun vm(
        wordRepository: FakeWordRepository = FakeWordRepository(words(30)),
        gameRepository: GameRepository = FakeGameRepository(),
        initialization: MutableStateFlow<AppInitializationState> = MutableStateFlow(AppInitializationState.Ready),
    ) = NewGameViewModel(wordRepository, FakeLogoStorage(), ValidateTeamsUseCase(), ValidateGameSettingsUseCase(), SelectWordsForNewGameUseCase(object : WordShuffler { override fun <T> shuffled(items: List<T>) = items }), GetAvailableWordsUseCase(wordRepository), CreateGameSessionUseCase(gameRepository), initialization)

    private fun makeValid(vm: NewGameViewModel) {
        vm.uiState.value.teams.forEachIndexed { index, team ->
            vm.updateTeamName(team.id, "Team $index")
            vm.updatePlayer1Name(team.id, "A$index")
            vm.updatePlayer2Name(team.id, "B$index")
        }
        vm.setWordCount(20)
        vm.setDifficulty(1)
    }

    private class FakeWordRepository(private val availableWords: List<Word>) : WordRepository {
        private val themes = (1..25).map { ThemeSummary("theme$it", if (it == 24) "Интим 18+" else if (it == 25) "Жёсткий 18+" else "Theme $it", 81, mapOf(1 to 27, 2 to 27, 3 to 27)) }
        override suspend fun ensureWordsImported() = com.finemooll.shelka.data.importer.ImportResult.AlreadyImported
        override suspend fun getAllWords() = availableWords
        override suspend fun getThemeSummaries() = themes
        override suspend fun getWordsByDifficultyAndThemes(difficulty: Int, themeIds: Set<String>) = availableWords.filter { it.difficulty == difficulty && it.themeId in themeIds }
        override suspend fun getAvailableWords(difficulty: Int, themeIds: Set<String>) = getWordsByDifficultyAndThemes(difficulty, themeIds)
    }
    private class FakeGameRepository : GameRepository { var calls = 0; override suspend fun createGame(draft: NewGameDraft, selectedWords: List<Word>): CreateGameResult { calls++; return CreateGameResult.Success("game-$calls") } }
    private class FakeLogoStorage : TeamLogoStorage { override suspend fun copyToInternalStorage(sourceUri: Uri, teamDraftId: String, previousDraftPath: String?) = LogoStorageResult.Success("internal"); override suspend fun adoptDraftLogo(draftPath: String?, gameId: String, teamId: String) = LogoStorageResult.Success(""); override suspend fun deleteAdoptedLogos(paths: Collection<String>) = Unit }
    companion object { fun words(count: Int) = (1..count).map { Word("w$it", "word", "theme1", "Theme 1", 1) } }
}
