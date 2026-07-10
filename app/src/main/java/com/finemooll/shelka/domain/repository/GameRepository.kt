package com.finemooll.shelka.domain.repository

import com.finemooll.shelka.domain.model.NewGameDraft
import com.finemooll.shelka.domain.model.Word
import com.finemooll.shelka.domain.usecase.CreateGameResult

interface GameRepository { suspend fun createGame(draft: NewGameDraft, selectedWords: List<Word>): CreateGameResult }
