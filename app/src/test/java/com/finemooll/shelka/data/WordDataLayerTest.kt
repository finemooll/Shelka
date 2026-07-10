package com.finemooll.shelka.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finemooll.shelka.data.asset.WordAssetDataSource
import com.finemooll.shelka.data.importer.*
import com.finemooll.shelka.data.json.*
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.local.entity.*
import com.finemooll.shelka.data.mapper.toDomain
import com.finemooll.shelka.data.mapper.toEntity
import com.finemooll.shelka.data.repository.WordRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class WordDataLayerTest {
    private val parser = WordJsonParser()
    private val validator = DefaultWordValidator()
    private lateinit var db: AppDatabase
    private lateinit var state: FakeState

    private class FakeAsset(private val text: String) : WordAssetDataSource { override suspend fun readWordsJson() = text }
    private class CancelAsset : WordAssetDataSource { override suspend fun readWordsJson(): String { throw CancellationException("cancel") } }
    private class FakeState(var imported: Boolean = false) : WordImportStateStore { override suspend fun isImported() = imported; override suspend fun setImported(imported: Boolean) { this.imported = imported } }

    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build(); state = FakeState() }
    @After fun tearDown() { db.close() }

    private fun realJson(): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return try {
            context.assets.open("words.json").bufferedReader().use { it.readText() }
        } catch (cause: IOException) {
            Assert.fail("Expected real app/src/main/assets/words.json to be available through test assets: ${cause.message}")
            error("unreachable")
        }
    }
    private fun realDtos() = parser.parse(realJson())
    private fun importer(json: String = realJson()) = WordImporter(FakeAsset(json), parser, validator, db, state)

    @Test fun realAssetParsesAndHasRequiredShape() {
        val words = realDtos()
        Assert.assertEquals(2025, words.size)
        Assert.assertEquals(words.size, words.map { it.id }.toSet().size)
        Assert.assertTrue(words.all { it.difficulty in 1..3 })
        Assert.assertEquals(25, words.map { it.themeId }.toSet().size)
        words.groupBy { it.themeId to it.difficulty }.values.forEach { Assert.assertEquals(27, it.size) }
        words.groupBy { it.themeId }.values.forEach { Assert.assertEquals(1, it.map { w -> w.themeName }.toSet().size) }
    }

    @Test fun validatorAcceptsRealAssetAndRejectsInvalidCases() {
        val words = realDtos()
        Assert.assertSame(WordValidationResult.Valid, validator.validate(words))
        fun invalid(mutator: (MutableList<WordDto>) -> Unit): List<WordValidationError> { val copy = words.toMutableList(); mutator(copy); return (validator.validate(copy) as WordValidationResult.Invalid).errors }
        Assert.assertTrue(invalid { it[1] = it[1].copy(id = it[0].id) }.any { it is WordValidationError.DuplicateId })
        Assert.assertTrue(invalid { it[0] = it[0].copy(id = " ") }.any { it is WordValidationError.BlankField && it.field == "id" })
        Assert.assertTrue(invalid { it[0] = it[0].copy(text = " ") }.any { it is WordValidationError.BlankField && it.field == "text" })
        Assert.assertTrue(invalid { it[0] = it[0].copy(themeId = " ") }.any { it is WordValidationError.BlankField && it.field == "themeId" })
        Assert.assertTrue(invalid { it[0] = it[0].copy(themeName = " ") }.any { it is WordValidationError.BlankField && it.field == "themeName" })
        Assert.assertTrue(invalid { it[0] = it[0].copy(difficulty = 4) }.any { it is WordValidationError.InvalidDifficulty })
    }

    @Test fun parserRejectsMalformedObjectAndNonArrayRoot() {
        Assert.assertThrows(WordJsonParseException::class.java) { parser.parse("{\"id\":\"x\"}") }
        val malformed = "[${'{'}\"id\":\"x\",\"text\":5,\"themeId\":\"t\",\"themeName\":\"T\",\"difficulty\":1}]"
        val error = Assert.assertThrows(WordJsonParseException::class.java) { parser.parse(malformed) }
        Assert.assertTrue(error.message!!.contains("index 0"))
    }

    @Test fun mappingsWork() {
        val dto = WordDto("id", "text", "theme", "Theme", 1)
        val entity = dto.toEntity()
        Assert.assertEquals("id", entity.id)
        Assert.assertEquals(dto, WordDto(entity.id, entity.text, entity.themeId, entity.themeName, entity.difficulty))
        Assert.assertEquals("text", entity.toDomain().text)
    }

    @Test fun roomInsertQueryAndThemeSummaryWork() = runTest {
        val entities = realDtos().take(81).map { it.toEntity() }
        db.wordDao().insertAll(entities)
        Assert.assertEquals(81, db.wordDao().count())
        Assert.assertEquals(27, db.wordDao().getByDifficulty(1).size)
        val theme = entities.first().themeId
        Assert.assertEquals(27, db.wordDao().getByDifficultyAndThemeIds(1, setOf(theme)).size)
        val summaries = WordRepositoryImpl(db.wordDao(), importer()).getThemeSummaries()
        Assert.assertEquals(1, summaries.size)
        Assert.assertEquals(mapOf(1 to 27, 2 to 27, 3 to 27), summaries.first().countByDifficulty)
    }

    @Test fun importScenariosAreSafeAndIdempotent() = runTest {
        val imp = importer()
        Assert.assertSame(ImportResult.Imported, imp.ensureImported())
        Assert.assertEquals(2025, db.wordDao().count())
        Assert.assertTrue(state.imported)
        Assert.assertSame(ImportResult.AlreadyImported, imp.ensureImported())
        state.imported = false
        Assert.assertSame(ImportResult.AlreadyImported, imp.ensureImported())
        Assert.assertTrue(state.imported)
        db.wordDao().clearForTest(); state.imported = true
        Assert.assertSame(ImportResult.Imported, imp.ensureImported())
        val missing = db.wordDao().getAll().first(); db.wordDao().deleteByIds(setOf(missing.id)); state.imported = false
        Assert.assertSame(ImportResult.Imported, imp.ensureImported())
        Assert.assertTrue(db.wordDao().getAllIds().contains(missing.id))
    }

    @Test fun incorrectIdsUnexpectedRowsAndValidationFailureAreHandled() = runTest {
        val all = realDtos().map { it.toEntity() }
        db.wordDao().insertAll(all.dropLast(1) + all.last().copy(id = "unexpected"))
        Assert.assertSame(ImportResult.Imported, importer().ensureImported())
        Assert.assertFalse(db.wordDao().getAllIds().contains("unexpected"))
        db.wordDao().clearForTest()
        val invalid = realDtos().toMutableList().also { it[0] = it[0].copy(id = " ") }
        val json = kotlinx.serialization.json.Json.encodeToString(ListSerializer(WordDto.serializer()), invalid)
        Assert.assertTrue(importer(json).ensureImported() is ImportResult.ValidationFailed)
        Assert.assertEquals(0, db.wordDao().count())
    }


    @Test fun finalVerificationFailureRollsBackRepairTransaction() = runTest {
        val canonicalId = realDtos().first().id
        val unexpected = realDtos().first().toEntity().copy(id = "unexpected")
        db.wordDao().insertAll(listOf(unexpected))
        val imp = WordImporter(
            assetDataSource = FakeAsset(realJson()),
            parser = parser,
            validator = validator,
            database = db,
            stateStore = state,
            canonicalWordVerifier = object : CanonicalWordVerifier {
                override suspend fun verify(database: AppDatabase, canonicalIds: Set<String>) {
                    database.wordDao().deleteByIds(setOf(canonicalId))
                    RoomCanonicalWordVerifier.verify(database, canonicalIds)
                }
            },
        )

        val result = imp.ensureImported()

        Assert.assertTrue(result is ImportResult.ConsistencyFailed)
        Assert.assertEquals(listOf("unexpected"), db.wordDao().getAllIds())
        Assert.assertFalse(state.imported)
    }

    @Test fun referencedUnexpectedWordReturnsConsistencyFailureAndPreservesHistory() = runTest {
        val canonical = realDtos().first().toEntity()
        val unexpected = canonical.copy(id = "unexpected")
        db.wordDao().insertAll(listOf(unexpected))
        createHistory("unexpected")
        val result = importer().ensureImported()
        Assert.assertTrue(result is ImportResult.ConsistencyFailed)
        Assert.assertTrue(db.wordDao().getAllIds().contains("unexpected"))
        Assert.assertEquals(1, db.gameHistoryDao().selectedWordCount("game", "unexpected"))
        Assert.assertEquals(1, db.gameHistoryDao().turnWordResultCount("turn", "unexpected"))
    }

    @Test fun repeatedImportPreservesSelectedWordsAndTurnResults() = runTest {
        val wordId = realDtos().first().id
        Assert.assertSame(ImportResult.Imported, importer().ensureImported())
        createHistory(wordId)
        Assert.assertSame(ImportResult.AlreadyImported, importer().ensureImported())
        Assert.assertEquals(1, db.gameHistoryDao().selectedWordCount("game", wordId))
        Assert.assertEquals(1, db.gameHistoryDao().turnWordResultCount("turn", wordId))
    }

    @Test fun cancellationIsRethrown() = runTest {
        val imp = WordImporter(CancelAsset(), parser, validator, db, state)
        try {
            imp.ensureImported()
            Assert.fail("CancellationException expected")
        } catch (expected: CancellationException) {
            Assert.assertEquals("cancel", expected.message)
        }
    }

    private suspend fun createHistory(wordId: String) {
        val h = db.gameHistoryDao()
        h.insertGameSession(GameSessionEntity("game", 1, 1, null, GameSessionStatus.IN_PROGRESS, 30, 1, 60, 0))
        h.insertTeam(TeamEntity("team", "game", "Team", null, 0))
        h.insertPlayer(PlayerEntity("p1", "team", "A", 0)); h.insertPlayer(PlayerEntity("p2", "team", "B", 1))
        h.insertSelectedWord(GameSelectedWordEntity("game", wordId, 0))
        h.insertTurn(TurnEntity("turn", "game", 0, "team", "p1", "p2", 1, null, true))
        h.insertTurnWordResult(TurnWordResultEntity("result", "turn", wordId, TurnWordResultStatus.CORRECT, 0))
    }
}
