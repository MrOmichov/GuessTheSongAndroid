package org.mromichov.guessthesong.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser

class PlaylistRepositoryImplTest {

    private val jsonConfig = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    private fun createRepository(
        mockEngine: MockEngine,
        spotifyUrlParser: SpotifyPlaylistUrlParser = SpotifyPlaylistUrlParser(),
        spotifyMapper: SpotifyMapper = SpotifyMapper()
    ): PlaylistRepositoryImpl {
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
        }
        val yandexApi = YandexApi(client)
        val yandexParser = YandexPlaylistUrlParser()
        val yandexMapper = YandexMapper()
        val spotifyApi = SpotifyApi(client)
        return PlaylistRepositoryImpl(
            yandexApi = yandexApi,
            yandexUrlParser = yandexParser,
            yandexMapper = yandexMapper,
            spotifyApi = spotifyApi,
            spotifyUrlParser = spotifyUrlParser,
            spotifyMapper = spotifyMapper
        )
    }

    @Test
    fun getPlaylistByUrl_userPlaylist_returnsSuccessPlaylist() = runTest {
        val mockJson = """
        {
            "result": {
                "title": "Top Hits",
                "tracks": [
                    {
                        "track": {
                            "id": "1",
                            "title": "Song 1",
                            "artists": [{ "name": "Artist 1" }]
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = createRepository(mockEngine)
        val result = repository.getPlaylistByUrl("https://music.yandex.ru/users/test/playlists/100")

        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertEquals("Top Hits", playlist?.title)
        assertEquals(1, playlist?.tracks?.size)
        assertEquals("Song 1", playlist?.tracks?.first()?.title)
    }

    @Test
    fun getPlaylistByUrl_uuidPlaylist_returnsSuccessPlaylist() = runTest {
        val mockJson = """
        {
            "result": {
                "title": "Uuid Hits",
                "tracks": [
                    {
                        "track": {
                            "id": "2",
                            "title": "Song 2",
                            "artists": [{ "name": "Artist 2" }]
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = createRepository(mockEngine)
        val result = repository.getPlaylistByUrl("https://music.yandex.ru/playlists/1234-uuid")

        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertEquals("Uuid Hits", playlist?.title)
        assertEquals(1, playlist?.tracks?.size)
        assertEquals("Song 2", playlist?.tracks?.first()?.title)
    }

    @Test
    fun getPlaylistByUrl_spotifyPlaylist_returnsSuccessPlaylist() = runTest {
        val tracksJson = (0 until 20).joinToString(",") { index ->
            """
            {
                "uri": "spotify:track:track_$index",
                "title": "Spotify Song $index",
                "subtitle": "Artist $index",
                "duration": 180000
            }
            """.trimIndent()
        }

        val mockHtml = """
        <!DOCTYPE html>
        <html>
        <body>
        <script id="__NEXT_DATA__" type="application/json">
        {
            "props": {
                "pageProps": {
                    "state": {
                        "data": {
                            "entity": {
                                "id": "37i9dQZF1DXcBWIGoYBM5M",
                                "name": "Today's Top Hits",
                                "trackList": [ $tracksJson ]
                            }
                        }
                    }
                }
            }
        }
        </script>
        </body>
        </html>
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockHtml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")
            )
        }

        val repository = createRepository(mockEngine)
        val result = repository.getPlaylistByUrl("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M")

        assertTrue(result.isSuccess)
        val playlist = result.getOrNull()
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", playlist?.id)
        assertEquals("Today's Top Hits", playlist?.title)
        assertEquals(20, playlist?.tracks?.size)
        assertEquals("Spotify Song 0", playlist?.tracks?.first()?.title)
    }

    @Test
    fun getPlaylistByUrl_invalidUrl_returnsFailure() = runTest {
        val mockEngine = MockEngine {
            respond("", HttpStatusCode.OK)
        }
        val repository = createRepository(mockEngine)
        val result = repository.getPlaylistByUrl("https://unknown.com/playlist")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
