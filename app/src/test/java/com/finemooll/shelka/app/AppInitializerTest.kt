package com.finemooll.shelka.app

import com.finemooll.shelka.data.importer.ImportResult
import com.finemooll.shelka.domain.model.ThemeSummary
import com.finemooll.shelka.domain.model.Word
import com.finemooll.shelka.domain.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppInitializerTest {
    private class Repo(private val result: ImportResult) : WordRepository {
        override suspend fun ensureWordsImported() = result
        override suspend fun getAllWords(): List<Word> = emptyList()
        override suspend fun getThemeSummaries(): List<ThemeSummary> = emptyList()
        override suspend fun getWordsByDifficultyAndThemes(difficulty: Int, themeIds: Set<String>): List<Word> = emptyList()
        override suspend fun getAvailableWords(difficulty: Int, themeIds: Set<String>): List<Word> = emptyList()
    }

    @Test fun exposesLoadingThenReady() {
        val initializer = AppInitializer(Repo(ImportResult.Imported), TestScope(UnconfinedTestDispatcher()))
        Assert.assertSame(AppInitializationState.Loading, initializer.state.value)
        initializer.start()
        Assert.assertSame(AppInitializationState.Ready, initializer.state.value)
    }

    @Test fun exposesErrorOnFailedImport() {
        val initializer = AppInitializer(Repo(ImportResult.Failed(IllegalStateException("boom"))), TestScope(UnconfinedTestDispatcher()))
        initializer.start()
        Assert.assertTrue(initializer.state.value is AppInitializationState.Error)
    }
}
