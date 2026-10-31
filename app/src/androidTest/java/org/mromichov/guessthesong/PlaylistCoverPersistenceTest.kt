package org.mromichov.guessthesong

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.core.database.PlaylistDatabase
import org.mromichov.guessthesong.core.database.PlaylistMigrations
import org.mromichov.guessthesong.data.local.FilePlaylistCoverStorage
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.data.repository.PlaylistRepositoryImpl
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlaylistCoverPersistenceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun coverFilesFollowPlaylistOwnershipAcrossReopenReplaceClearAndDelete() = runBlocking {
        val suffix = System.nanoTime()
        val databaseName = "playlist-cover-integration-$suffix.db"
        val coverDirectory = File(context.cacheDir, "playlist-cover-integration-$suffix")
        val imageBytes = createPng()
        var requestCount = 0
        val client = HttpClient(MockEngine {
            requestCount++
            respond(
                content = imageBytes,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "image/png"),
            )
        })
        val storage = FilePlaylistCoverStorage(coverDirectory, client) { file ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, options)
            options.outWidth > 0 && options.outHeight > 0
        }
        var database = openDatabase(databaseName)

        try {
            var repository = repository(database, client, storage)
            val coverContent = PlaylistContent(
                title = "First copy",
                tracks = emptyList(),
                coverUrl = "https://covers.test/shared.png",
            )
            val first = repository.savePlaylist(coverContent, PlaylistProvider.SPOTIFY).getOrThrow()
            val second = repository.savePlaylist(
                coverContent.copy(title = "Second copy"),
                PlaylistProvider.YANDEX_MUSIC,
            ).getOrThrow()
            val firstFile = File(coverDirectory, requireNotNull(first.coverFileName))
            val secondFile = File(coverDirectory, requireNotNull(second.coverFileName))
            assertNotEquals(first.coverFileName, second.coverFileName)
            assertTrue(firstFile.exists())
            assertTrue(secondFile.exists())
            assertEquals(2, requestCount)

            database.close()
            database = openDatabase(databaseName)
            repository = repository(database, client, storage)
            val reopened = repository.getLocalPlaylists().getOrThrow().associateBy { it.id }
            assertEquals(2, reopened.size)
            assertEquals(PlaylistProvider.SPOTIFY, reopened.getValue(first.id).provider)
            assertEquals(PlaylistProvider.YANDEX_MUSIC, reopened.getValue(second.id).provider)
            assertEquals(first.coverFileName, reopened.getValue(first.id).coverFileName)
            assertEquals(second.coverFileName, reopened.getValue(second.id).coverFileName)
            assertEquals("Local reads must not fetch playlist or cover URLs", 2, requestCount)

            val replaced = repository.replacePlaylist(
                first.id,
                coverContent.copy(title = "First replaced"),
                PlaylistProvider.YANDEX_MUSIC,
            ).getOrThrow()
            assertNotEquals(first.coverFileName, replaced.coverFileName)
            assertFalse(firstFile.exists())
            assertTrue(secondFile.exists())
            assertTrue(File(coverDirectory, requireNotNull(replaced.coverFileName)).exists())
            assertEquals(PlaylistProvider.YANDEX_MUSIC, replaced.provider)

            val cleared = repository.replacePlaylist(
                second.id,
                PlaylistContent("Second without cover", emptyList()),
                PlaylistProvider.SPOTIFY,
            ).getOrThrow()
            assertEquals(null, cleared.coverFileName)
            assertFalse(secondFile.exists())
            assertEquals(PlaylistProvider.SPOTIFY, cleared.provider)
            assertEquals(listOf(first.id, second.id).sorted(), repository.getLocalPlaylists().getOrThrow().map { it.id }.sorted())

            repository.deletePlaylist(first.id).getOrThrow()
            assertFalse(File(coverDirectory, requireNotNull(replaced.coverFileName)).exists())
            repository.deletePlaylist(second.id).getOrThrow()
            assertEquals(emptyList<Long>(), repository.getLocalPlaylists().getOrThrow().map { it.id })
            assertTrue(coverDirectory.listFiles().orEmpty().isEmpty())
            assertEquals(3, requestCount)
        } finally {
            database.close()
            client.close()
            coverDirectory.deleteRecursively()
            context.deleteDatabase(databaseName)
        }
    }

    private fun openDatabase(name: String) = Room.databaseBuilder(
        context,
        PlaylistDatabase::class.java,
        name,
    ).addMigrations(PlaylistMigrations.MIGRATION_1_2).build()

    private fun repository(
        database: PlaylistDatabase,
        client: HttpClient,
        storage: FilePlaylistCoverStorage,
    ) = PlaylistRepositoryImpl(
        yandexApi = YandexApi(client),
        yandexUrlParser = YandexPlaylistUrlParser(),
        yandexMapper = YandexMapper(),
        spotifyApi = SpotifyApi(client),
        spotifyUrlParser = SpotifyPlaylistUrlParser(),
        spotifyMapper = SpotifyMapper(),
        playlistTracksDao = database.playlistTracksDao(),
        coverStorage = storage,
    )

    private fun createPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.MAGENTA)
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }
}
