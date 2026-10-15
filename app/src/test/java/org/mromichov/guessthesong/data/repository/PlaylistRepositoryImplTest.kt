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
import org.mromichov.guessthesong.data.dao.FakePlaylistTracksDao
import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track

class PlaylistRepositoryImplTest {

    private val jsonConfig = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    private fun createRepository(
        mockEngine: MockEngine,
        spotifyUrlParser: SpotifyPlaylistUrlParser = SpotifyPlaylistUrlParser(),
        spotifyMapper: SpotifyMapper = SpotifyMapper(),
        dao: FakePlaylistTracksDao = FakePlaylistTracksDao(),
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
            spotifyMapper = spotifyMapper,
            playlistTracksDao = dao,
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
        val saved = repository.getSavedPlaylists().getOrThrow().single()
        assertEquals("Top Hits", saved.title)
        assertEquals("Song 1", saved.tracks.single().title)
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
        val saved = repository.getSavedPlaylists().getOrThrow().single()
        assertEquals("Uuid Hits", saved.title)
        assertEquals("Song 2", saved.tracks.single().title)
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
        val saved = repository.getSavedPlaylists().getOrThrow().single()
        assertEquals("Today's Top Hits", saved.title)
        assertEquals(20, saved.tracks.size)
        assertEquals("Spotify Song 0", saved.tracks.first().title)
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

    @Test
    fun getSavedPlaylist_readsLocalDataWithoutNetworkRequest() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            error("Network should not be used for local playlist reads")
        }
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(
                playlist = PlaylistEntity(id = 42L, title = "Saved locally"),
                tracks = listOf(TrackEntity(title = "Local song", artist = "Local artist")),
            ),
        )
        val repository = createRepository(mockEngine, dao = dao)

        val result = repository.getSavedPlaylist(42L)

        assertTrue(result.isSuccess)
        assertEquals("42", result.getOrThrow().id)
        assertEquals("Saved locally", result.getOrThrow().title)
        assertEquals("Local song", result.getOrThrow().tracks.single().title)
        assertEquals(0, requestCount)
    }

    @Test
    fun fetchPlaylistByUrl_doesNotPersistToDao() = runTest {
        val mockJson = """
        {
            "result": {
                "title": "Remote Only",
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

        val dao = FakePlaylistTracksDao()
        val repository = createRepository(mockEngine, dao = dao)
        val result = repository.fetchPlaylistByUrl("https://music.yandex.ru/users/test/playlists/100")

        assertTrue(result.isSuccess)
        assertEquals("Remote Only", result.getOrThrow().title)
        assertTrue("DAO should remain empty after fetchPlaylistByUrl", dao.getSavedPlaylists().isEmpty())
    }

    @Test
    fun findSavedPlaylistByTitle_returnsMatchingSavedPlaylistCaseInsensitively() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(
                playlist = PlaylistEntity(id = 10L, title = "Rock Classics"),
                tracks = listOf(TrackEntity(title = "Song A", artist = "Band A")),
            ),
        )
        val repository = createRepository(MockEngine { error("No network") }, dao = dao)

        val found = repository.findSavedPlaylistByTitle("rock classics").getOrThrow()
        val notFound = repository.findSavedPlaylistByTitle("Pop Hits").getOrThrow()

        assertEquals("10", found?.id)
        assertEquals("Rock Classics", found?.title)
        assertEquals(1, found?.tracks?.size)
        assertNull(notFound)
    }

    @Test
    fun savePlaylist_persistsPlaylistAndReturnsWithGeneratedId() = runTest {
        val dao = FakePlaylistTracksDao()
        val repository = createRepository(MockEngine { error("No network") }, dao = dao)

        val toSave = Playlist(
            id = "temporary-id",
            title = "My Custom Playlist",
            tracks = listOf(Track("Song X", "Artist X", null, 0L))
        )
        val result = repository.savePlaylist(toSave)

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals("My Custom Playlist", saved.title)
        assertEquals("1", saved.id)
        val fromDb = dao.getSavedPlaylist(1L)
        assertEquals("My Custom Playlist", fromDb?.playlist?.title)
        assertEquals("Song X", fromDb?.tracks?.single()?.title)
    }

    @Test
    fun replacePlaylist_updatesTracksForExistingId() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(
                playlist = PlaylistEntity(id = 5L, title = "Old Playlist"),
                tracks = listOf(TrackEntity(title = "Old Song", artist = "Old Artist")),
            ),
        )
        val repository = createRepository(MockEngine { error("No network") }, dao = dao)

        val updatedPlaylist = Playlist(
            id = "ignored",
            title = "New Playlist Name",
            tracks = listOf(
                Track("New Song 1", "Artist 1", null, 0L),
                Track("New Song 2", "Artist 2", null, 0L)
            )
        )
        val result = repository.replacePlaylist(5L, updatedPlaylist)

        assertTrue(result.isSuccess)
        assertEquals("5", result.getOrThrow().id)
        assertEquals("New Playlist Name", result.getOrThrow().title)
        val fromDb = dao.getSavedPlaylist(5L)
        assertEquals("New Playlist Name", fromDb?.playlist?.title)
        assertEquals(2, fromDb?.tracks?.size)
        assertEquals("New Song 1", fromDb?.tracks?.first()?.title)
    }

    @Test
    fun deletePlaylist_removesPlaylistFromDao() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(
                playlist = PlaylistEntity(id = 7L, title = "To Delete"),
                tracks = listOf(TrackEntity(title = "Song", artist = "Artist")),
            ),
        )
        val repository = createRepository(MockEngine { error("No network") }, dao = dao)

        val result = repository.deletePlaylist(7L)

        assertTrue(result.isSuccess)
        assertNull(dao.getSavedPlaylist(7L))
        assertTrue(dao.getSavedPlaylists().isEmpty())
    }
}
