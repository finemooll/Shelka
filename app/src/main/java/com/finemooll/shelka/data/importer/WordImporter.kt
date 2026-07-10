package com.finemooll.shelka.data.importer

import androidx.room.withTransaction
import com.finemooll.shelka.data.asset.WordAssetDataSource
import com.finemooll.shelka.data.json.*
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.mapper.toEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private class CanonicalRepairRollbackException(val error: WordDatabaseConsistencyError) : RuntimeException()

sealed interface ImportResult {
    data object Imported : ImportResult
    data object AlreadyImported : ImportResult
    data class ValidationFailed(val errors: List<WordValidationError>) : ImportResult
    data class ConsistencyFailed(val reason: WordDatabaseConsistencyError) : ImportResult
    data class Failed(val cause: Throwable) : ImportResult
}

sealed interface WordDatabaseConsistencyError {
    data class UnexpectedReferencedWords(val wordIds: Set<String>) : WordDatabaseConsistencyError
    data class FinalCanonicalMismatch(val missingIds: Set<String>, val unexpectedIds: Set<String>) : WordDatabaseConsistencyError
}

class WordImporter(
    private val assetDataSource: WordAssetDataSource,
    private val parser: WordJsonParser,
    private val validator: WordValidator,
    private val database: AppDatabase,
    private val stateStore: WordImportStateStore,
    private val mutex: Mutex = Mutex(),
    private val postRepairHook: suspend AppDatabase.() -> Unit = {},
) {
    suspend fun ensureImported(): ImportResult = mutex.withLock {
        try {
            val raw = assetDataSource.readWordsJson()
            val dtos = parser.parse(raw)
            when (val validation = validator.validate(dtos)) {
                WordValidationResult.Valid -> Unit
                is WordValidationResult.Invalid -> return@withLock ImportResult.ValidationFailed(validation.errors)
            }
            val canonical = dtos.map { it.id }.toSet()
            val importedFlag = stateStore.isImported()
            val persisted = database.wordDao().getAllIds().toSet()
            if (persisted == canonical) {
                if (!importedFlag) stateStore.setImported(true)
                return@withLock ImportResult.AlreadyImported
            }
            val unexpected = persisted - canonical
            if (unexpected.isNotEmpty()) {
                val referenced = database.gameHistoryDao().getReferencedWordIds(unexpected).toSet()
                if (referenced.isNotEmpty()) return@withLock ImportResult.ConsistencyFailed(WordDatabaseConsistencyError.UnexpectedReferencedWords(referenced))
            }
            val entities = dtos.map { it.toEntity() }
            try {
                database.withTransaction {
                    if (unexpected.isNotEmpty()) database.wordDao().deleteByIds(unexpected)
                    database.wordDao().insertAll(entities)
                    database.wordDao().updateAll(entities)
                    postRepairHook(database)
                    val finalIds = database.wordDao().getAllIds().toSet()
                    if (finalIds != canonical || database.wordDao().count() != canonical.size) {
                        throw CanonicalRepairRollbackException(
                            WordDatabaseConsistencyError.FinalCanonicalMismatch(
                                missingIds = canonical - finalIds,
                                unexpectedIds = finalIds - canonical,
                            )
                        )
                    }
                }
            } catch (cause: CanonicalRepairRollbackException) {
                return@withLock ImportResult.ConsistencyFailed(cause.error)
            }
            stateStore.setImported(true)
            ImportResult.Imported
        } catch (cause: CancellationException) { throw cause }
        catch (cause: Throwable) { ImportResult.Failed(cause) }
    }
}
