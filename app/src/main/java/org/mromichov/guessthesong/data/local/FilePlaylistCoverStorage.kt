package org.mromichov.guessthesong.data.local

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Url
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.UUID

class FilePlaylistCoverStorage(
    private val directory: File,
    private val httpClient: HttpClient,
    private val isValidImage: (File) -> Boolean,
) : PlaylistCoverStorage {
    override suspend fun save(url: String): String? = withContext(Dispatchers.IO) {
        var temporary: File? = null
        var completed: File? = null
        var returned = false
        try {
            val parsed = Url(url)
            if (parsed.protocol.name !in setOf("http", "https")) return@withContext null
            check(directory.mkdirs() || directory.isDirectory)
            val name = UUID.randomUUID().toString()
            val staging = ownedFile("$name.tmp").also { temporary = it }
            val downloaded = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                httpClient.prepareGet(url).execute { response ->
                    if (response.status.value !in 200..299) return@execute false
                    val channel = response.bodyAsChannel()
                    staging.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var size = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = channel.readAvailable(buffer, 0, buffer.size)
                            if (count == -1) break
                            size += count
                            if (size > MAX_BYTES) throw IOException("Playlist cover exceeds size limit")
                            output.write(buffer, 0, count)
                        }
                    }
                    true
                }
            } ?: false
            if (!downloaded || !isValidImage(staging)) return@withContext null
            currentCoroutineContext().ensureActive()
            val target = ownedFile("$name.img")
            if (!staging.renameTo(target)) throw IOException("Cannot finalize playlist cover")
            completed = target
            returned = true
            target.name
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        } finally {
            temporary?.delete()
            if (!returned) completed?.delete()
            // Cancellation after returning to the caller can leave an orphan; reconcile removes it.
        }
    }

    override suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        val file = ownedFile(fileName)
        if (file.exists() && !file.delete()) throw IOException("Cannot delete playlist cover")
        Unit
    }

    override suspend fun reconcile(referencedFileNames: Set<String>) = withContext(Dispatchers.IO) {
        if (!directory.exists()) return@withContext
        val files = directory.listFiles() ?: throw IOException("Cannot list playlist covers")
        for (file in files) {
            currentCoroutineContext().ensureActive()
            if (file.name !in referencedFileNames && file.isFile) delete(file.name)
        }
    }

    private fun ownedFile(name: String): File {
        require(name.isNotBlank() && name != "." && name != ".." && '/' !in name && '\\' !in name)
        return File(directory, name).also {
            require(it.canonicalFile.parentFile == directory.canonicalFile) { "Invalid cover path" }
        }
    }

    companion object {
        const val DIRECTORY_NAME = "playlist-covers"
        const val MAX_BYTES = 5L * 1024 * 1024
        const val DOWNLOAD_TIMEOUT_MS = 15_000L
    }
}
