package com.finemooll.shelka.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.local.entity.GameSelectedWordEntity
import com.finemooll.shelka.data.local.entity.GameSessionEntity
import com.finemooll.shelka.data.local.entity.GameSessionStatus
import com.finemooll.shelka.data.local.entity.TeamEntity
import com.finemooll.shelka.data.local.entity.WordEntity
import com.finemooll.shelka.data.logo.LogoStorageResult
import com.finemooll.shelka.data.logo.TeamLogoStorage
import com.finemooll.shelka.data.repository.RoomGameRepository
import com.finemooll.shelka.domain.model.*
import com.finemooll.shelka.domain.usecase.CreateGameResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomGameRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var logos: FakeLogoStorage
    private lateinit var repo: RoomGameRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        logos = FakeLogoStorage()
        repo = RoomGameRepository(db, logos)
    }
    @After fun tearDown() { db.close() }

    @Test fun successfulCreationPersistsGameTeamsPlayersAndOrderedWords() = runTest {
        seedWords(20)
        val result = repo.createGame(draft(), words(20)) as CreateGameResult.Success
        val game = db.gameHistoryDao().getGameSession(result.gameId)!!
        assertEquals(GameSessionStatus.IN_PROGRESS, game.status)
        assertNull(game.completedAt)
        assertEquals(0, game.currentRoundIndex)
        assertEquals(2, db.gameHistoryDao().teamCount(result.gameId))
        val teams = db.gameHistoryDao().getTeamsForGame(result.gameId)
        assertEquals(listOf("t1", "t2"), teams.map { it.id })
        assertEquals(listOf("p1", "p2"), db.gameHistoryDao().getPlayersForTeam("t1").map { it.id })
        assertEquals(listOf(0, 1), db.gameHistoryDao().getPlayersForTeam("t1").map { it.orderIndex })
        assertEquals(20, db.gameHistoryDao().selectedWordCountForGame(result.gameId))
        assertEquals((0 until 20).toList(), db.gameHistoryDao().getSelectedWordsForGame(result.gameId).map { it.selectedOrder })
    }

    @Test fun invalidDraftDuplicateWordsAndAlreadyUsedWordsCreateNoRows() = runTest {
        seedWords(20)
        assertTrue(repo.createGame(draft().copy(teams = listOf(team("only"))), words(20)) is CreateGameResult.InvalidDraft)
        assertEquals(0, db.gameHistoryDao().gameSessionCount())
        assertTrue(repo.createGame(draft(), words(20).let { it.dropLast(1) + it.first() }) is CreateGameResult.InvalidDraft)
        assertEquals(0, db.gameHistoryDao().gameSessionCount())
        db.gameHistoryDao().insertGameSession(GameSessionEntity("existing", 1, 1, null, GameSessionStatus.IN_PROGRESS, 20, 1, 60, 0))
        db.gameHistoryDao().insertTeam(TeamEntity("existing-team", "existing", "Existing", null, 0))
        db.gameHistoryDao().insertSelectedWord(GameSelectedWordEntity("existing", "w1", 0))
        assertTrue(repo.createGame(draft(), words(20)) is CreateGameResult.WordConflict)
        assertEquals(1, db.gameHistoryDao().gameSessionCount())
    }

    @Test fun failedTransactionKeepsDraftLogoDeletesStableCopyAndRetrySucceeds() = runTest {
        seedWords(20)
        val draft = draftWithLogo("draft-logo")
        db.gameHistoryDao().insertGameSession(GameSessionEntity("existing", 1, 1, null, GameSessionStatus.IN_PROGRESS, 20, 1, 60, 0))
        db.gameHistoryDao().insertTeam(TeamEntity("existing-team", "existing", "Existing", null, 0))
        db.gameHistoryDao().insertSelectedWord(GameSelectedWordEntity("existing", "w1", 0))

        assertTrue(repo.createGame(draft, words(20)) is CreateGameResult.WordConflict)
        assertTrue("draft must survive failed creation", logos.drafts.contains("draft-logo"))
        assertTrue("stable copy must be cleaned after failure", logos.stableDeleted.isNotEmpty())

        val retryWords = words(20).drop(1) + Word("w21", "word21", "theme1", "Theme 1", 1)
        db.wordDao().insertAll(listOf(WordEntity("w21", "word21", "theme1", "Theme 1", 1)))
        val result = repo.createGame(draft, retryWords) as CreateGameResult.Success
        val persistedLogo = db.gameHistoryDao().getTeamsForGame(result.gameId).first().logoPath!!
        assertFalse(persistedLogo.contains("team_logos/drafts"))
        assertFalse("successful creation removes draft", logos.drafts.contains("draft-logo"))
    }

    @Test fun logoStorageFailureIsStructuredLogoFailure() = runTest {
        seedWords(20)
        logos.failPrepare = true
        val result = repo.createGame(draftWithLogo("draft-logo"), words(20))
        assertTrue(result is CreateGameResult.LogoFailure)
        assertEquals(0, db.gameHistoryDao().gameSessionCount())
    }

    @Test fun cancellationIsRethrownAndAdoptedLogosAreCleanedOnFailure() = runTest {
        seedWords(20)
        logos.cancel = true
        try {
            repo.createGame(draft(), words(20))
            fail("CancellationException expected")
        } catch (expected: CancellationException) {
            assertEquals("cancel", expected.message)
        }
        assertEquals(0, db.gameHistoryDao().gameSessionCount())
    }

    private suspend fun seedWords(count: Int) { db.wordDao().insertAll(words(count).map { WordEntity(it.id, it.text, it.themeId, it.themeName, it.difficulty) }) }
    private fun words(count: Int) = (1..count).map { Word("w$it", "word$it", "theme1", "Theme 1", 1) }
    private fun team(id: String) = TeamDraft(id, "Team $id", PlayerDraft("${id}p1", "A$id"), PlayerDraft("${id}p2", "B$id"), null)
    private fun draft() = NewGameDraft(listOf(TeamDraft("t1", "Team 1", PlayerDraft("p1", "Ann"), PlayerDraft("p2", "Bob"), null), TeamDraft("t2", "Team 2", PlayerDraft("p3", "Cat"), PlayerDraft("p4", "Dan"), null)), GameSettingsDraft(20, 1, 60, setOf("theme1")))
    private fun draftWithLogo(path: String) = draft().copy(teams = draft().teams.mapIndexed { index, team -> if (index == 0) team.copy(logoPath = path) else team })

    private class FakeLogoStorage : TeamLogoStorage {
        var cancel = false
        var failPrepare = false
        val drafts = mutableSetOf("draft-logo")
        val stableDeleted = mutableListOf<String>()
        override suspend fun copyToInternalStorage(sourceUri: android.net.Uri, teamDraftId: String, previousDraftPath: String?) = LogoStorageResult.Success("draft")
        override suspend fun prepareStableLogo(draftPath: String?, gameId: String, teamId: String): LogoStorageResult {
            if (cancel) throw CancellationException("cancel")
            if (failPrepare) return LogoStorageResult.Failure("logo failed", IllegalStateException("io"))
            return LogoStorageResult.Success(draftPath?.let { "team_logos/games/$gameId/$teamId/logo" }.orEmpty())
        }
        override suspend fun deleteStableLogos(paths: Collection<String>) { stableDeleted += paths }
        override suspend fun deleteDraftLogos(paths: Collection<String>) { drafts.removeAll(paths.toSet()) }
    }
}
