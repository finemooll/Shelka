package com.finemooll.shelka.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.local.entity.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordAvailabilityTest {
    private lateinit var db: AppDatabase

    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun tearDown() { db.close() }

    @Test fun availabilityExcludesPersistedUnfinishedGameWordsAndRespectsDifficultyAndThemes() = runTest {
        db.wordDao().insertAll(listOf(
            WordEntity("w1", "one", "theme1", "Theme 1", 1),
            WordEntity("w2", "two", "theme1", "Theme 1", 1),
            WordEntity("w3", "three", "theme2", "Theme 2", 1),
            WordEntity("w4", "four", "theme1", "Theme 1", 2),
        ))
        db.gameHistoryDao().insertGameSession(GameSessionEntity("game", 1, 1, null, GameSessionStatus.IN_PROGRESS, 20, 1, 60, 0))
        db.gameHistoryDao().insertTeam(TeamEntity("team", "game", "Team", null, 0))
        db.gameHistoryDao().insertSelectedWord(GameSelectedWordEntity("game", "w1", 0))

        val available = db.wordDao().getAvailableByDifficultyAndThemeIds(1, setOf("theme1"))

        Assert.assertEquals(listOf("w2"), available.map { it.id })
    }
}
