package com.finemooll.shelka.data.logo

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface LogoStorageResult { data class Success(val internalPath: String) : LogoStorageResult; data class Failure(val message: String, val cause: Throwable? = null) : LogoStorageResult }

interface TeamLogoStorage {
    suspend fun copyToInternalStorage(sourceUri: Uri, teamDraftId: String, previousDraftPath: String? = null): LogoStorageResult
    suspend fun adoptDraftLogo(draftPath: String?, gameId: String, teamId: String): LogoStorageResult
    suspend fun deleteAdoptedLogos(paths: Collection<String>)
}

class AndroidTeamLogoStorage(private val context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : TeamLogoStorage {
    private val root: File get() = File(context.filesDir, "team_logos")
    private val draftsRoot: File get() = File(root, "drafts")
    private val gamesRoot: File get() = File(root, "games")

    override suspend fun copyToInternalStorage(sourceUri: Uri, teamDraftId: String, previousDraftPath: String?): LogoStorageResult = withContext(ioDispatcher) {
        try {
            val stream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext LogoStorageResult.Failure("Не удалось прочитать выбранный логотип")
            val dir = File(draftsRoot, safeSegment(teamDraftId)).apply { mkdirs() }
            val target = File(dir, "${UUID.randomUUID()}.logo")
            stream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            deleteIfUnderRoot(previousDraftPath, draftsRoot)
            LogoStorageResult.Success(target.canonicalPath)
        } catch (cancel: CancellationException) { throw cancel }
        catch (t: Throwable) { LogoStorageResult.Failure("Не удалось сохранить логотип", t) }
    }

    override suspend fun adoptDraftLogo(draftPath: String?, gameId: String, teamId: String): LogoStorageResult = withContext(ioDispatcher) {
        if (draftPath == null) return@withContext LogoStorageResult.Success("")
        try {
            val source = File(draftPath).canonicalFile
            if (!source.isFile || !isUnderRoot(source, draftsRoot)) return@withContext LogoStorageResult.Failure("Логотип команды должен быть файлом черновика")
            val targetDir = File(File(gamesRoot, safeSegment(gameId)), safeSegment(teamId)).apply { mkdirs() }
            val target = File(targetDir, source.name)
            if (!isUnderRoot(target, gamesRoot)) return@withContext LogoStorageResult.Failure("Некорректный путь логотипа")
            if (!source.renameTo(target)) {
                source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                source.delete()
            }
            LogoStorageResult.Success(target.canonicalPath)
        } catch (cancel: CancellationException) { throw cancel }
        catch (t: Throwable) { LogoStorageResult.Failure("Не удалось закрепить логотип", t) }
    }

    override suspend fun deleteAdoptedLogos(paths: Collection<String>) = withContext(ioDispatcher) {
        paths.forEach { deleteIfUnderRoot(it, gamesRoot) }
    }

    private fun safeSegment(value: String): String = value.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifBlank { UUID.randomUUID().toString() }
    private fun isUnderRoot(file: File, expectedRoot: File): Boolean = file.canonicalPath.startsWith(expectedRoot.canonicalPath + File.separator)
    private fun deleteIfUnderRoot(path: String?, expectedRoot: File) { if (path != null) File(path).canonicalFile.takeIf { isUnderRoot(it, expectedRoot) }?.delete() }
}
