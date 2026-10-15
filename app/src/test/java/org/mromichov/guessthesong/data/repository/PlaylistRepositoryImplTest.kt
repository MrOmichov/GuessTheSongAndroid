package org.mromichov.guessthesong.data.repository

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.mromichov.guessthesong.data.dao.FakePlaylistTracksDao
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.domain.model.PlaylistContent
import org.mromichov.guessthesong.domain.model.PlaylistProvider
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

        val result = repository.getLocalPlaylist(42L)

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrThrow().id)
        assertEquals("Saved locally", result.getOrThrow().content.title)
        assertEquals("Local song", result.getOrThrow().content.tracks.single().title)
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
        assertEquals(PlaylistProvider.YANDEX_MUSIC, result.getOrThrow().source.provider)
        assertEquals("test:100", result.getOrThrow().source.externalId)
        assertEquals("Remote Only", result.getOrThrow().content.title)
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

        val found = repository.findLocalPlaylistByTitle("rock classics").getOrThrow()
        val notFound = repository.findLocalPlaylistByTitle("Pop Hits").getOrThrow()

        assertEquals(10L, found?.id)
        assertEquals("Rock Classics", found?.content?.title)
        assertEquals(1, found?.content?.tracks?.size)
        assertNull(notFound)
    }

    @Test
    fun savePlaylist_persistsPlaylistAndReturnsWithGeneratedId() = runTest {
        val dao = FakePlaylistTracksDao()
        val repository = createRepository(MockEngine { error("No network") }, dao = dao)

        val toSave = PlaylistContent(
            title = "My Custom Playlist",
            tracks = listOf(Track("Song X", "Artist X", null, 0L))
        )
        val result = repository.savePlaylist(toSave)

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals("My Custom Playlist", saved.content.title)
        assertEquals(1L, saved.id)
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

        val updatedPlaylist = PlaylistContent(
            title = "New Playlist Name",
            tracks = listOf(
                Track("New Song 1", "Artist 1", null, 0L),
                Track("New Song 2", "Artist 2", null, 0L)
            )
        )
        val result = repository.replacePlaylist(5L, updatedPlaylist)

        assertTrue(result.isSuccess)
        assertEquals(5L, result.getOrThrow().id)
        assertEquals("New Playlist Name", result.getOrThrow().content.title)
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
