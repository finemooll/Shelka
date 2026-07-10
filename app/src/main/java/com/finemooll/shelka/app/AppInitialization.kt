package com.finemooll.shelka.app

import com.finemooll.shelka.data.importer.ImportResult
import com.finemooll.shelka.domain.repository.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AppInitializationState {
    data object Loading : AppInitializationState
    data object Ready : AppInitializationState
    data class Error(val cause: Throwable?, val message: String) : AppInitializationState
}

class AppInitializer(private val repository: WordRepository, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow<AppInitializationState>(AppInitializationState.Loading)
    val state: StateFlow<AppInitializationState> = mutableState

    fun start() {
        mutableState.value = AppInitializationState.Loading
        scope.launch {
            mutableState.value = when (val result = repository.ensureWordsImported()) {
                ImportResult.Imported, ImportResult.AlreadyImported -> AppInitializationState.Ready
                is ImportResult.ValidationFailed -> AppInitializationState.Error(null, "Word asset validation failed: ${result.errors.size} error(s)")
                is ImportResult.ConsistencyFailed -> AppInitializationState.Error(null, "Word database consistency check failed: ${result.reason}")
                is ImportResult.Failed -> AppInitializationState.Error(result.cause, "Word import failed")
            }
        }
    }
}
