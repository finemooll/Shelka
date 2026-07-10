# PR3 new-game flow

PR3 introduces the setup flow only. Active turns, timers, scoring, score editing, round transitions, history management, resume, statistics, sound, and vibration remain PR4/PR5 work.

## Architecture

Draft state lives in `NewGameViewModel` and is represented by immutable draft models (`TeamDraft`, `PlayerDraft`, `GameSettingsDraft`). Draft IDs are generated once and are not list indexes. Composables render `NewGameUiState` and emit explicit ViewModel events; validation, word selection, file copy, SQL, and transaction logic stay outside Composables.

## Draft vs persisted data

Draft teams and settings are editable until game creation succeeds. After creation the persisted `GameSession`, `TeamEntity`, `PlayerEntity`, and `GameSelectedWordEntity` rows are treated as immutable setup data for later gameplay.

The first round is stored with `currentRoundIndex = 0`, matching zero-based round indexing for future PR4 gameplay.

## Validation

`ValidateTeamsUseCase` enforces 2–8 teams, exactly two player fields per team, non-blank team/player names, unique non-blank IDs, and distinct player names within a team after trimming/case normalization.

`ValidateGameSettingsUseCase` allows only word counts 20–100 by tens, difficulty 1–3, timer 20–180 seconds by tens, at least one selected theme, and only theme IDs loaded from Room.

`RoomGameRepository` repeats critical creation-boundary checks before touching Room: team count, names, IDs, settings, selected-word count, unique selected word IDs, selected-word difficulty, and selected-word theme membership. Invalid drafts return a structured failure and insert no rows.

## Logos

`TeamLogoStorage` copies a selected Photo Picker `Uri` into internal app storage under a draft logo directory using generated safe filenames. The external URI is never the final `logoPath`. Replacing a draft logo removes the previous draft file only after canonical-path checks against the draft root.

Successful game creation adopts draft logos into `team_logos/games/<gameId>/<teamId>/...` and persists only that stable internal path. If adoption succeeds but creation later fails, newly adopted stable files are cleaned up. Persisted game logos are never deleted by draft replacement cleanup.

## Word availability and selection

Available words come from Room through `WordRepository` and are filtered by difficulty, selected themes, and a `NOT EXISTS` exclusion against `game_selected_words`, so words already selected by any persisted game are not reused.

`SelectWordsForNewGameUseCase` injects shuffling for deterministic tests. When all 25 themes are selected and 50 words are requested, it selects exactly 2 words per theme. When all 25 themes are selected and 100 words are requested, it selects exactly 4 words per theme. Other combinations randomly select from eligible words without forced equal distribution.

Inside the creation transaction, selected word IDs are rechecked against `game_selected_words`. A race that makes a selected word unavailable returns a structured conflict and rolls back the entire game creation.

If there are not enough eligible words, no game rows are inserted and the UI shows the required insufficient-words message with an `Открыть историю` action.

## Persistence

`RoomGameRepository.createGame` performs one Room transaction. It inserts the game session, ordered teams, exactly two ordered players per team, and selected words with `(gameId, wordId)` rows. Any failure rolls back the whole setup. Coroutine cancellation is rethrown instead of converted into an ordinary failure.
