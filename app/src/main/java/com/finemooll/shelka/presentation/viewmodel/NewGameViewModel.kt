package com.finemooll.shelka.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finemooll.shelka.app.AppInitializationState
import com.finemooll.shelka.data.logo.LogoStorageResult
import com.finemooll.shelka.data.logo.TeamLogoStorage
import com.finemooll.shelka.domain.model.*
import com.finemooll.shelka.domain.repository.WordRepository
import com.finemooll.shelka.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

private fun newTeam(): TeamDraft { val t = UUID.randomUUID().toString(); return TeamDraft(t, "", PlayerDraft(UUID.randomUUID().toString(), ""), PlayerDraft(UUID.randomUUID().toString(), ""), null) }

data class ThemeUiModel(val id: String, val name: String)
data class NewGameValidationErrors(val teams: TeamValidationErrors = TeamValidationErrors(), val settings: SettingsValidationErrors = SettingsValidationErrors())
data class NewGameUiState(
    val teams: List<TeamDraft> = listOf(newTeam(), newTeam()),
    val wordCount: Int = 50,
    val difficulty: Int = 1,
    val timerSeconds: Int = 60,
    val availableThemes: List<ThemeUiModel> = emptyList(),
    val selectedThemeIds: Set<String> = emptySet(),
    val validationErrors: NewGameValidationErrors = NewGameValidationErrors(),
    val isLoading: Boolean = false,
    val status: NewGameStatus = NewGameStatus.LoadingThemes,
    val insufficientWords: Boolean = false,
    val createdGameId: String? = null,
    val fatalError: String? = null,
)
sealed interface NewGameStatus { data object LoadingThemes: NewGameStatus; data object EditingTeams: NewGameStatus; data object EditingSettings: NewGameStatus; data object Creating: NewGameStatus; data object Created: NewGameStatus; data class Error(val message: String): NewGameStatus }

class NewGameViewModel(
    private val wordRepository: WordRepository,
    private val logoStorage: TeamLogoStorage,
    private val validateTeams: ValidateTeamsUseCase,
    private val validateSettings: ValidateGameSettingsUseCase,
    private val selectWords: SelectWordsForNewGameUseCase,
    private val getAvailableWords: GetAvailableWordsUseCase,
    private val createGame: CreateGameSessionUseCase,
    private val appInitializationState: StateFlow<AppInitializationState>,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NewGameUiState())
    val uiState: StateFlow<NewGameUiState> = mutableState.asStateFlow()

    init { loadThemes() }
    private fun themesDomain() = mutableState.value.availableThemes.map { Theme(it.id, it.name) }
    private fun settings() = GameSettingsDraft(mutableState.value.wordCount, mutableState.value.difficulty, mutableState.value.timerSeconds, mutableState.value.selectedThemeIds)
    private fun revalidate() { mutableState.update { it.copy(validationErrors = NewGameValidationErrors(validateTeams(it.teams), validateSettings(settings(), themesDomain()))) } }

    fun loadThemes() = viewModelScope.launch {
        mutableState.update { it.copy(isLoading = true, status = NewGameStatus.LoadingThemes) }
        try {
            val themes = wordRepository.getThemeSummaries().map { ThemeUiModel(it.themeId, it.themeName) }.distinctBy { it.id }
            mutableState.update { it.copy(availableThemes = themes, selectedThemeIds = themes.map { t -> t.id }.toSet(), isLoading = false, status = NewGameStatus.EditingTeams) }
            revalidate()
        } catch (cancel: CancellationException) { throw cancel }
        catch (t: Throwable) { mutableState.update { it.copy(isLoading = false, fatalError = "Не удалось загрузить темы", status = NewGameStatus.Error("Не удалось загрузить темы")) } }
    }
    fun addTeam() { mutableState.update { if (it.teams.size >= NewGameRules.maxTeams) it else it.copy(teams = it.teams + newTeam()) }; revalidate() }
    fun removeTeam(teamId: String) { mutableState.update { it.copy(teams = it.teams.filterNot { t -> t.id == teamId }) }; revalidate() }
    fun updateTeamName(teamId: String, name: String) { updateTeam(teamId) { it.copy(name = name) } }
    fun updatePlayer1Name(teamId: String, name: String) { updateTeam(teamId) { it.copy(player1 = it.player1.copy(name = name)) } }
    fun updatePlayer2Name(teamId: String, name: String) { updateTeam(teamId) { it.copy(player2 = it.player2.copy(name = name)) } }
    private fun updateTeam(teamId: String, f: (TeamDraft) -> TeamDraft) { mutableState.update { it.copy(teams = it.teams.map { t -> if (t.id == teamId) f(t) else t }) }; revalidate() }
    fun chooseLogo(teamId: String, uri: Uri) = viewModelScope.launch {
        val team = mutableState.value.teams.firstOrNull { it.id == teamId } ?: return@launch
        mutableState.update { it.copy(isLoading = true) }
        when (val result = logoStorage.copyToInternalStorage(uri, teamId, team.logoPath)) {
            is LogoStorageResult.Success -> updateTeam(teamId) { it.copy(logoPath = result.internalPath) }
            is LogoStorageResult.Failure -> mutableState.update { it.copy(fatalError = result.message, status = NewGameStatus.Error(result.message)) }
        }
        mutableState.update { it.copy(isLoading = false) }
    }
    fun setWordCount(v: Int) { mutableState.update { it.copy(wordCount = v) }; revalidate() }
    fun setDifficulty(v: Int) { mutableState.update { it.copy(difficulty = v) }; revalidate() }
    fun setTimer(v: Int) { mutableState.update { it.copy(timerSeconds = v) }; revalidate() }
    fun toggleTheme(id: String) { mutableState.update { val set = it.selectedThemeIds.toMutableSet(); if (!set.add(id)) set.remove(id); it.copy(selectedThemeIds = set) }; revalidate() }
    fun selectAllThemes() { mutableState.update { it.copy(selectedThemeIds = it.availableThemes.map { t -> t.id }.toSet()) }; revalidate() }
    fun clearThemes() { mutableState.update { it.copy(selectedThemeIds = emptySet()) }; revalidate() }
    fun continueToSettings(): Boolean { revalidate(); val ok = mutableState.value.validationErrors.teams.isValid; if (ok) mutableState.update { it.copy(status = NewGameStatus.EditingSettings) }; return ok }
    fun backToTeams() { mutableState.update { it.copy(status = NewGameStatus.EditingTeams) } }
    fun dismissError() { mutableState.update { it.copy(fatalError = null, status = if (it.status is NewGameStatus.Error) NewGameStatus.EditingSettings else it.status) } }
    fun startGame() = viewModelScope.launch {
        if (mutableState.value.status == NewGameStatus.Creating || mutableState.value.createdGameId != null) return@launch
        if (appInitializationState.value !is AppInitializationState.Ready) { mutableState.update { it.copy(fatalError = "Слова ещё не готовы. Подождите завершения загрузки.") }; return@launch }
        revalidate(); val state = mutableState.value
        if (!state.validationErrors.teams.isValid || !state.validationErrors.settings.isValid) return@launch
        mutableState.update { it.copy(isLoading = true, status = NewGameStatus.Creating, insufficientWords = false) }
        val available = getAvailableWords(state.difficulty, state.selectedThemeIds)
        when (val selected = selectWords(state.wordCount, state.difficulty, state.selectedThemeIds, available)) {
            is WordSelectionResult.InsufficientWords -> mutableState.update { it.copy(isLoading = false, insufficientWords = true, fatalError = NewGameRules.insufficientWordsMessage, status = NewGameStatus.EditingSettings) }
            is WordSelectionResult.Success -> when (val result = createGame(NewGameDraft(state.teams, settings()), selected.words)) {
                is CreateGameResult.Success -> mutableState.update { it.copy(isLoading = false, createdGameId = result.gameId, status = NewGameStatus.Created) }
                is CreateGameResult.InvalidDraft -> mutableState.update { it.copy(isLoading = false, fatalError = "Проверьте настройки игры", status = NewGameStatus.EditingSettings) }
                is CreateGameResult.WordConflict -> mutableState.update { it.copy(isLoading = false, fatalError = "Некоторые слова уже использованы. Повторите запуск игры.", status = NewGameStatus.EditingSettings) }
                is CreateGameResult.Failure -> mutableState.update { it.copy(isLoading = false, fatalError = "Не удалось создать игру", status = NewGameStatus.Error("Не удалось создать игру")) }
            }
        }
    }
}

class NewGameViewModelFactory(private val deps: Dependencies) : ViewModelProvider.Factory {
    data class Dependencies(val wordRepository: WordRepository, val logoStorage: TeamLogoStorage, val createGame: CreateGameSessionUseCase, val appInitializationState: StateFlow<AppInitializationState>)
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = NewGameViewModel(deps.wordRepository, deps.logoStorage, ValidateTeamsUseCase(), ValidateGameSettingsUseCase(), SelectWordsForNewGameUseCase(), GetAvailableWordsUseCase(deps.wordRepository), deps.createGame, deps.appInitializationState) as T
}
