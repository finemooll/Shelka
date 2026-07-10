package com.finemooll.shelka.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.local.entity.WordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: WordDao
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build(); dao = db.wordDao() }
    @After fun tearDown() = db.close()
    @Test fun insertAndQueryWordsAndThemeRows() = runTest {
        dao.insertAll(listOf(WordEntity("a", "A", "t1", "Theme 1", 1), WordEntity("b", "B", "t1", "Theme 1", 2), WordEntity("c", "C", "t2", "Theme 2", 1)))
        assertEquals(3, dao.count())
        assertEquals(listOf("a", "c"), dao.getByDifficulty(1).map { it.id })
        assertEquals(listOf("a"), dao.getByDifficultyAndThemeIds(1, setOf("t1")).map { it.id })
        assertEquals(3, dao.getThemeSummaryRows().sumOf { it.count })
    }
}
