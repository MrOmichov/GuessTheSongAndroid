package org.mromichov.guessthesong.presentation.game.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class CoilTrackCoverLoaderTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun validImageIsDecodedIntoPreparedCoverWithoutWritingFiles() = runBlocking {
        var cacheControl: String? = null
        val client = client(pngBytes(800, 600)) { request ->
            cacheControl = request.headers[HttpHeaders.CacheControl]
        }
        val imageLoader = ImageLoader.Builder(context).diskCache(null).build()
        val loader = CoilTrackCoverLoader(context, client, imageLoader)
        val before = appFilesSnapshot()

        try {
            val result = loader.load("https://art.test/cover.png")

            assertNotNull(result)
            val prepared = result as CoilPreparedCover
            assertTrue(prepared.image.width in 1..400)
            assertTrue(prepared.image.height in 1..400)
            assertEquals("no-store", cacheControl)
            assertEquals(before, appFilesSnapshot())
        } finally {
            imageLoader.shutdown()
            client.close()
        }
    }

    @Test
    fun invalidImageReturnsNullWithoutWritingFiles() = runBlocking {
        val client = client("not an image".toByteArray())
        val imageLoader = ImageLoader.Builder(context).diskCache(null).build()
        val loader = CoilTrackCoverLoader(context, client, imageLoader)
        val before = appFilesSnapshot()

        try {
            assertNull(loader.load("https://art.test/broken.png"))
            assertEquals(before, appFilesSnapshot())
        } finally {
            imageLoader.shutdown()
            client.close()
        }
    }

    private fun client(bytes: ByteArray, onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {}) =
        HttpClient(MockEngine { request ->
            onRequest(request)
            respond(
                content = bytes,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "image/png"),
            )
        })

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.MAGENTA)
        canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN })
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun appFilesSnapshot(): Map<String, Long> = listOf("files" to context.filesDir, "cache" to context.cacheDir)
        .flatMap { (name, root) ->
            root.walkTopDown()
                .filter(File::isFile)
                .map { "$name/${it.relativeTo(root).path}" to it.length() }
                .toList()
        }
        .associate { it }
}
