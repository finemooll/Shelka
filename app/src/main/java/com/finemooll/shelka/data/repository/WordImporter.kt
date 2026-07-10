package com.finemooll.shelka.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.room.withTransaction
import com.finemooll.shelka.data.asset.WordAssetDataSource
import com.finemooll.shelka.data.json.WordJsonParser
import com.finemooll.shelka.data.json.WordValidationResult
import com.finemooll.shelka.data.json.WordValidator
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.mapper.toEntity
import com.finemooll.shelka.domain.repository.ImportResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val ExpectedImportedWordCount = 2025
private val WordsImportedKey = booleanPreferencesKey("words_imported")

class WordImporter(
    private val assetDataSource: WordAssetDataSource,
    private val parser: WordJsonParser,
    private val validator: WordValidator,
    private val database: AppDatabase,
    private val dataStore: DataStore<Preferences>,
) {
    private val mutex = Mutex()

    suspend fun ensureWordsImported(): ImportResult = mutex.withLock {
        try {
            val imported = dataStore.data.first()[WordsImportedKey] == true
            val count = database.wordDao().count()
            if (imported && count == ExpectedImportedWordCount) return@withLock ImportResult.AlreadyImported

            val dtos = parser.parse(assetDataSource.readWordsJson())
            when (val result = validator.validate(dtos)) {
                WordValidationResult.Valid -> Unit
                is WordValidationResult.Invalid -> return@withLock ImportResult.ValidationFailed(result.errors)
            }
            database.withTransaction { database.wordDao().insertAll(dtos.map { it.toEntity() }) }
            if (database.wordDao().count() != ExpectedImportedWordCount) {
                return@withLock ImportResult.Failed(IllegalStateException("Imported word count did not match $ExpectedImportedWordCount"))
            }
            dataStore.edit { it[WordsImportedKey] = true }
            ImportResult.Imported
        } catch (cause: Throwable) {
            ImportResult.Failed(cause)
        }
    }
}
