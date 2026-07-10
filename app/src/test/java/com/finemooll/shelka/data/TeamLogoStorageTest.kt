package com.finemooll.shelka.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.finemooll.shelka.data.logo.AndroidTeamLogoStorage
import com.finemooll.shelka.data.logo.LogoStorageResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TeamLogoStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun copiesBytesInternallyAndReplacingDraftDeletesPreviousDraft() = runTest {
        val storage = AndroidTeamLogoStorage(context)
        val source1 = textFile("source1.txt", "first")
        val first = storage.copyToInternalStorage(Uri.fromFile(source1), "team/../one") as LogoStorageResult.Success
        assertTrue(first.internalPath.startsWith(File(context.filesDir, "team_logos").canonicalPath))
        assertFalse(first.internalPath.startsWith("file:"))
        assertEquals("first", File(first.internalPath).readText())

        val source2 = textFile("source2.txt", "second")
        val second = storage.copyToInternalStorage(Uri.fromFile(source2), "team-one", first.internalPath) as LogoStorageResult.Success
        assertFalse(File(first.internalPath).exists())
        assertEquals("second", File(second.internalPath).readText())
    }

    @Test fun missingStreamReturnsFailureAndAdoptionMovesDraftToStableGamePath() = runTest {
        val storage = AndroidTeamLogoStorage(context)
        assertTrue(storage.copyToInternalStorage(Uri.parse("content://missing/logo"), "team") is LogoStorageResult.Failure)
        val draft = storage.copyToInternalStorage(Uri.fromFile(textFile("source3.txt", "logo")), "team") as LogoStorageResult.Success
        val adopted = storage.prepareStableLogo(draft.internalPath, "game/../1", "team/../1") as LogoStorageResult.Success
        assertFalse(adopted.internalPath.contains("team_logos/drafts"))
        assertTrue(adopted.internalPath.contains("team_logos/games"))
        assertTrue(File(draft.internalPath).exists())
        assertEquals("logo", File(adopted.internalPath).readText())
        storage.deleteStableLogos(listOf(adopted.internalPath))
        assertFalse(File(adopted.internalPath).exists())
        storage.deleteDraftLogos(listOf(draft.internalPath))
        assertFalse(File(draft.internalPath).exists())
    }

    private fun textFile(name: String, value: String): File = File(context.cacheDir, name).apply { writeText(value) }
}
