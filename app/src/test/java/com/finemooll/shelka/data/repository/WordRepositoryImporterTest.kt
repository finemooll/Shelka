package com.finemooll.shelka.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finemooll.shelka.data.asset.WordAssetDataSource
import com.finemooll.shelka.data.json.DefaultWordValidator
import com.finemooll.shelka.data.json.KotlinxWordJsonParser
import com.finemooll.shelka.data.json.WordDto
import com.finemooll.shelka.data.json.WordValidationError
import com.finemooll.shelka.data.json.WordValidationResult
import com.finemooll.shelka.data.json.WordValidator
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.domain.repository.ImportResult
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordRepositoryImporterTest {
    private lateinit var db: AppDatabase
    private lateinit var tempDir: File
    private val realJson = File("src/main/assets/words.json").readText()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        tempDir = createTempDir()
    }
    @After fun tearDown() { db.close(); tempDir.deleteRecursively() }

    private fun importer(validator: WordValidator = DefaultWordValidator()) = WordImporter(
        assetDataSource = object : WordAssetDataSource { override suspend fun readWordsJson() = realJson },
        parser = KotlinxWordJsonParser(),
        validator = validator,
        database = db,
        dataStore = PreferenceDataStoreFactory.create { File(tempDir, "prefs.preferences_pb") },
    )

    @Test fun importIsIdempotent() = runTest {
        val importer = importer()
        assertSame(ImportResult.Imported, importer.ensureWordsImported())
        assertEquals(2025, db.wordDao().count())
        assertSame(ImportResult.AlreadyImported, importer.ensureWordsImported())
        assertEquals(2025, db.wordDao().count())
    }

    @Test fun failedValidationDoesNotInsertPartialData() = runTest {
        val importer = importer(object : WordValidator {
            override fun validate(words: List<WordDto>) = WordValidationResult.Invalid(listOf(WordValidationError.UnexpectedWordCount(2025, 0)))
        })
        val result = importer.ensureWordsImported()
        assertTrue(result is ImportResult.ValidationFailed)
        assertEquals(0, db.wordDao().count())
    }

    @Test fun repositoryAggregatesThemeSummaries() = runTest {
        val importer = importer()
        val repository = WordRepositoryImpl(db.wordDao(), importer)
        assertSame(ImportResult.Imported, repository.ensureWordsImported())
        val summaries = repository.getThemeSummaries()
        assertEquals(25, summaries.size)
        assertTrue(summaries.all { it.count == 81 })
        assertTrue(summaries.all { it.countByDifficulty == mapOf(1 to 27, 2 to 27, 3 to 27) })
        assertEquals(27, repository.getAvailableWords(1, setOf(summaries.first().themeId)).size)
    }
}
