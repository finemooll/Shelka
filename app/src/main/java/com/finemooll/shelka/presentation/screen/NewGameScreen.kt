package com.finemooll.shelka.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finemooll.shelka.ShelkaApplication
import com.finemooll.shelka.domain.usecase.NewGameRules
import com.finemooll.shelka.presentation.viewmodel.*

@Composable
fun NewGameScreen(onBackClick: () -> Unit, onHistoryClick: () -> Unit, onCreated: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as ShelkaApplication
    val vm: NewGameViewModel = viewModel(factory = NewGameViewModelFactory(app.appContainer.newGameDependencies))
    val state by vm.uiState.collectAsState()
    LaunchedEffect(state.createdGameId) { state.createdGameId?.let(onCreated) }
    when (state.status) {
        NewGameStatus.EditingTeams, NewGameStatus.LoadingThemes -> TeamsStep(state, vm, onBackClick)
        NewGameStatus.EditingSettings, NewGameStatus.Creating, is NewGameStatus.Error -> SettingsStep(state, vm, onHistoryClick)
        NewGameStatus.Created -> Text("Создание игры…")
    }
}

@Composable private fun TeamsStep(state: NewGameUiState, vm: NewGameViewModel, onBack: () -> Unit) {
    Scaffold(bottomBar = { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Button(onClick = onBack) { Text("Назад") }; Button(onClick = { vm.continueToSettings() }, enabled = state.validationErrors.teams.isValid && !state.isLoading) { Text("Далее") } } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Команды", style = MaterialTheme.typography.headlineMedium); state.validationErrors.teams.teamCount?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            items(state.teams, key = { it.id }) { team -> TeamCard(team, state.validationErrors.teams.perTeam[team.id], vm, state.teams.size > 2) }
            item { Button(onClick = vm::addTeam, enabled = state.teams.size < NewGameRules.maxTeams) { Text("Добавить команду") } }
        }
    }
}

@Composable private fun TeamCard(team: com.finemooll.shelka.domain.model.TeamDraft, errors: com.finemooll.shelka.domain.usecase.TeamFieldErrors?, vm: NewGameViewModel, canDelete: Boolean) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.chooseLogo(team.id, it) } }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(team.name, { vm.updateTeamName(team.id, it) }, label = { Text("Название команды") }, isError = errors?.teamName != null, supportingText = { errors?.teamName?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(team.player1.name, { vm.updatePlayer1Name(team.id, it) }, label = { Text("Игрок 1") }, isError = errors?.player1Name != null, supportingText = { errors?.player1Name?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(team.player2.name, { vm.updatePlayer2Name(team.id, it) }, label = { Text("Игрок 2") }, isError = errors?.player2Name != null || errors?.duplicatePlayers != null, supportingText = { Text(errors?.player2Name ?: errors?.duplicatePlayers ?: "") }, modifier = Modifier.fillMaxWidth())
        Text(if (team.logoPath == null) "Логотип: стандартный" else "Логотип выбран")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Выбрать логотип") }; OutlinedButton(onClick = { vm.removeTeam(team.id) }, enabled = canDelete) { Text("Удалить") } }
    } }
}

@Composable private fun SettingsStep(state: NewGameUiState, vm: NewGameViewModel, onHistory: () -> Unit) {
    Scaffold(bottomBar = { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Button(onClick = vm::backToTeams, enabled = !state.isLoading) { Text("Назад") }; Button(onClick = vm::startGame, enabled = state.validationErrors.settings.isValid && state.validationErrors.teams.isValid && !state.isLoading) { Text(if (state.isLoading) "Создание…" else "Начать игру") } } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Настройки игры", style = MaterialTheme.typography.headlineMedium) }
            item { ChoiceRow("Количество слов", NewGameRules.allowedWordCounts.toList(), state.wordCount, vm::setWordCount) }
            item { ChoiceRow("Сложность", NewGameRules.allowedDifficulties.toList(), state.difficulty, vm::setDifficulty) }
            item { TimerSlider(state.timerSeconds, vm::setTimer) }
            item { Text("Темы", style = MaterialTheme.typography.titleLarge); state.validationErrors.settings.selectedThemes?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Row { TextButton(vm::selectAllThemes) { Text("Выбрать все") }; TextButton(vm::clearThemes) { Text("Снять все") } } }
            items(state.availableThemes, key = { it.id }) { theme -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(theme.name); Checkbox(theme.id in state.selectedThemeIds, { _ -> vm.toggleTheme(theme.id) }) } }
            if (state.insufficientWords) item { Text(NewGameRules.insufficientWordsMessage, color = MaterialTheme.colorScheme.error); Button(onClick = onHistory) { Text("Открыть историю") } }
            state.fatalError?.let { item { Text(it, color = MaterialTheme.colorScheme.error); TextButton(vm::dismissError) { Text("Закрыть") } } }
        }
    }
}

@Composable private fun ChoiceRow(label: String, values: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            items(values, key = { it }) { value -> FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(value.toString()) }) }
        }
    }
}

@Composable private fun TimerSlider(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Таймер: $selected секунд")
        Slider(
            value = selected.toFloat(),
            onValueChange = { value -> onSelect((value / 10).toInt().coerceIn(2, 18) * 10) },
            valueRange = 20f..180f,
            steps = 15,
        )
    }
}

@Composable fun FirstRoundPlaceholderScreen(onBackClick: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), verticalArrangement = Arrangement.Center) {
            Text("I Раунд — Элиас", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text("Объясняйте словами. Нельзя называть само слово, однокоренные слова и говорить по буквам.")
            Spacer(Modifier.height(16.dp))
            Text("Игровой процесс будет реализован в PR4.")
            Spacer(Modifier.height(32.dp))
            Button(onClick = onBackClick) { Text("Назад") }
        }
    }
}
