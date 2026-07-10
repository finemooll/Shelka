package com.finemooll.shelka.data.repository

import com.finemooll.shelka.data.importer.ImportResult
import com.finemooll.shelka.data.importer.WordImporter
import com.finemooll.shelka.data.local.dao.WordDao
import com.finemooll.shelka.data.mapper.toDomain
import com.finemooll.shelka.domain.model.ThemeSummary
import com.finemooll.shelka.domain.model.Word
import com.finemooll.shelka.domain.repository.WordRepository

internal class WordRepositoryImpl(private val wordDao: WordDao, private val importer: WordImporter) : WordRepository {
    override suspend fun ensureWordsImported(): ImportResult = importer.ensureImported()
    override suspend fun getAllWords(): List<Word> = wordDao.getAll().map { it.toDomain() }
    override suspend fun getThemeSummaries(): List<ThemeSummary> = wordDao.getThemeSummaryRows()
        .groupBy { it.themeId to it.themeName }
        .map { (key, rows) -> ThemeSummary(key.first, key.second, rows.sumOf { it.count }, rows.associate { it.difficulty to it.count }) }
        .sortedBy { it.themeName }
    override suspend fun getWordsByDifficultyAndThemes(difficulty: Int, themeIds: Set<String>): List<Word> =
        if (themeIds.isEmpty()) emptyList() else wordDao.getByDifficultyAndThemeIds(difficulty, themeIds).map { it.toDomain() }
    override suspend fun getAvailableWords(difficulty: Int, themeIds: Set<String>): List<Word> =
        if (themeIds.isEmpty()) emptyList() else wordDao.getAvailableByDifficultyAndThemeIds(difficulty, themeIds).map { it.toDomain() }
}
