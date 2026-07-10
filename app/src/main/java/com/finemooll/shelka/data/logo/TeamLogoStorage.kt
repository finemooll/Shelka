package com.finemooll.shelka.data.logo

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface LogoStorageResult { data class Success(val internalPath: String) : LogoStorageResult; data class Failure(val message: String, val cause: Throwable? = null) : LogoStorageResult }

interface TeamLogoStorage {
    suspend fun copyToInternalStorage(sourceUri: Uri, teamDraftId: String, previousDraftPath: String? = null): LogoStorageResult
}

class AndroidTeamLogoStorage(private val context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : TeamLogoStorage {
    override suspend fun copyToInternalStorage(sourceUri: Uri, teamDraftId: String, previousDraftPath: String?): LogoStorageResult = withContext(ioDispatcher) {
        try {
            val stream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext LogoStorageResult.Failure("Не удалось прочитать выбранный логотип")
            val safeTeam = teamDraftId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifBlank { "team" }
            val dir = File(context.filesDir, "team_logos/drafts/$safeTeam").apply { mkdirs() }
            val target = File(dir, "${UUID.randomUUID()}.logo")
            stream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            previousDraftPath?.let { path -> File(path).takeIf { it.absolutePath.contains("team_logos/drafts") }?.delete() }
            LogoStorageResult.Success(target.absolutePath)
        } catch (t: Throwable) { LogoStorageResult.Failure("Не удалось сохранить логотип", t) }
    }
}
