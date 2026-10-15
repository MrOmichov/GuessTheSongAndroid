package org.mromichov.guessthesong.data.local

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FilePlaylistCoverStorageTest {
    private val directory = Files.createTempDirectory("playlist-covers-test").toFile()
    private val clients = mutableListOf<HttpClient>()

    @After
    fun cleanup() {
        clients.forEach { it.close() }
        directory.deleteRecursively()
    }

    @Test
    fun save_writesValidatedImageToPrivateDirectoryAndReturnsOnlyFileName() = runTest {
        val storage = storage(HttpStatusCode.OK, byteArrayOf(1, 2, 3), isValidImage = { it.length() == 3L })

        val fileName = storage.save("https://covers.test/cover.jpg")

        assertNotNull(fileName)
        assertEquals(fileName, File(directory, fileName!!).name)
        assertEquals(byteArrayOf(1, 2, 3).toList(), File(directory, fileName).readBytes().toList())
        assertEquals(listOf(fileName), directory.listFiles()?.map { it.name })
    }

    @Test
    fun save_returnsNullAndRemovesTemporaryFileForHttpErrorOrInvalidImage() = runTest {
        val httpError = storage(HttpStatusCode.ServiceUnavailable, byteArrayOf(1), isValidImage = { true })
        val invalidImage = storage(HttpStatusCode.OK, byteArrayOf(1), isValidImage = { false })

        assertNull(httpError.save("https://covers.test/unavailable.jpg"))
        assertNull(invalidImage.save("https://covers.test/not-image.jpg"))
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun reconcileDeletesOrphanFilesAndDeleteRejectsPathTraversal() = runTest {
        val storage = storage(HttpStatusCode.OK, byteArrayOf(1), isValidImage = { true })
        val kept = File(directory, "kept.img").apply { writeBytes(byteArrayOf(1)) }
        val orphan = File(directory, "orphan.tmp").apply { writeBytes(byteArrayOf(1)) }

        storage.reconcile(setOf(kept.name))

        assertTrue(kept.exists())
        assertFalse(orphan.exists())
        assertNull(storage.save("file:///outside/image.jpg"))
        try {
            storage.delete("../outside.img")
            throw AssertionError("Path traversal should be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: storage accepts only names owned by its directory.
        }
    }

    @Test
    fun saveRejectsPayloadOverLimitAndRemovesStagingFile() = runTest {
        val storage = storage(
            HttpStatusCode.OK,
            ByteArray(FilePlaylistCoverStorage.MAX_BYTES.toInt() + 1) { 7 },
            isValidImage = { true },
        )

        assertNull(storage.save("https://covers.test/too-large.jpg"))
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun savePropagatesCancellationAndRemovesStagingFile() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine {
            requestStarted.complete(Unit)
            awaitCancellation()
        }).also(clients::add)
        val storage = FilePlaylistCoverStorage(directory, client) { true }
        val pending = async { storage.save("https://covers.test/cancel.jpg") }

        requestStarted.await()
        pending.cancel()
        try {
            pending.await()
            throw AssertionError("Cancellation should propagate")
        } catch (_: CancellationException) {
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        }
    }

    private fun storage(
        status: HttpStatusCode,
        body: ByteArray,
        isValidImage: (File) -> Boolean,
    ) = FilePlaylistCoverStorage(
        directory = directory,
        httpClient = HttpClient(MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "image/jpeg"),
            )
        }).also(clients::add),
        isValidImage = isValidImage,
    )
}
