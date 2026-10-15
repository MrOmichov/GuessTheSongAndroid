package org.mromichov.guessthesong.data.repository

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.mromichov.guessthesong.data.dao.FakePlaylistTracksDao
import org.mromichov.guessthesong.data.local.PlaylistCoverStorage
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider

@OptIn(ExperimentalCoroutinesApi::class)
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
        covers: FakePlaylistCoverStorage = FakePlaylistCoverStorage(),
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
            coverStorage = covers,
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
                playlist = PlaylistEntity(
                    id = 42L,
                    title = "Saved locally",
                    coverFileName = "saved-cover.img",
                    provider = "SPOTIFY",
                ),
                tracks = listOf(TrackEntity(title = "Local song", artist = "Local artist")),
            ),
        )
        val repository = createRepository(mockEngine, dao = dao)

        val result = repository.getLocalPlaylist(42L)

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrThrow().id)
        assertEquals("Saved locally", result.getOrThrow().content.title)
        assertEquals("Local song", result.getOrThrow().content.tracks.single().title)
        assertNull(result.getOrThrow().content.coverUrl)
        assertEquals("saved-cover.img", result.getOrThrow().coverFileName)
        assertEquals(PlaylistProvider.SPOTIFY, result.getOrThrow().provider)
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
    fun savePlaylist_downloadsCoverAndPersistsProviderAndFileName() = runTest {
        val dao = FakePlaylistTracksDao()
        val covers = FakePlaylistCoverStorage(savedFileNames = ArrayDeque(listOf("cover-1.img")))
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)
        val content = PlaylistContent("With cover", emptyList(), coverUrl = "https://covers.test/one.jpg")

        val saved = repository.savePlaylist(content, PlaylistProvider.SPOTIFY).getOrThrow()

        assertEquals(listOf(content.coverUrl), covers.savedUrls)
        assertEquals("cover-1.img", saved.coverFileName)
        assertEquals(PlaylistProvider.SPOTIFY, saved.provider)
        assertEquals("cover-1.img", dao.getSavedPlaylist(saved.id)?.playlist?.coverFileName)
        assertEquals("SPOTIFY", dao.getSavedPlaylist(saved.id)?.playlist?.provider)
    }

    @Test
    fun savingSameCoverUrlCreatesIndependentOwnedFiles() = runTest {
        val dao = FakePlaylistTracksDao()
        val covers = FakePlaylistCoverStorage(savedFileNames = ArrayDeque(listOf("copy-1.img", "copy-2.img")))
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)
        val content = PlaylistContent("Copy", emptyList(), "https://covers.test/shared.jpg")

        val first = repository.savePlaylist(content).getOrThrow()
        val second = repository.savePlaylist(content).getOrThrow()

        assertTrue(first.id != second.id)
        assertEquals("copy-1.img", first.coverFileName)
        assertEquals("copy-2.img", second.coverFileName)
        assertEquals(listOf(content.coverUrl, content.coverUrl), covers.savedUrls)
        assertTrue(covers.deletedNames.isEmpty())
    }

    @Test
    fun replacePlaylist_updatesCoverAndProviderThenDeletesOldCover() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(
                playlist = PlaylistEntity(id = 5L, title = "Old", coverFileName = "old.img", provider = "SPOTIFY"),
                tracks = emptyList(),
            ),
        )
        val covers = FakePlaylistCoverStorage(savedFileNames = ArrayDeque(listOf("new.img")))
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)
        val content = PlaylistContent("New", emptyList(), coverUrl = "https://covers.test/new.jpg")

        val result = repository.replacePlaylist(5L, content, PlaylistProvider.YANDEX_MUSIC).getOrThrow()

        assertEquals(5L, result.id)
        assertEquals("new.img", result.coverFileName)
        assertEquals(PlaylistProvider.YANDEX_MUSIC, result.provider)
        assertEquals("new.img", dao.getSavedPlaylist(5L)?.playlist?.coverFileName)
        assertEquals("YANDEX_MUSIC", dao.getSavedPlaylist(5L)?.playlist?.provider)
        assertEquals(listOf("old.img"), covers.deletedNames)
    }

    @Test
    fun replacePlaylist_withoutNewCoverClearsOldCoverAndDeletesIt() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(PlaylistEntity(id = 8L, title = "Old", coverFileName = "old.img"), emptyList()),
        )
        val covers = FakePlaylistCoverStorage()
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)

        val result = repository.replacePlaylist(8L, PlaylistContent("No cover", emptyList())).getOrThrow()

        assertNull(result.coverFileName)
        assertNull(dao.getSavedPlaylist(8L)?.playlist?.coverFileName)
        assertEquals(listOf("old.img"), covers.deletedNames)
        assertTrue(covers.savedUrls.isEmpty())
    }

    @Test
    fun failedDatabaseWriteDeletesDownloadedFileAndLeavesPlaylistUnchanged() = runTest {
        val dao = FakePlaylistTracksDao().apply { failPlaylistWrite = true }
        val covers = FakePlaylistCoverStorage(savedFileNames = ArrayDeque(listOf("orphan.img")))
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)
        val result = repository.savePlaylist(
            PlaylistContent("Write fails", emptyList(), "https://covers.test/fail.jpg"),
        )

        assertTrue(result.isFailure)
        assertTrue(dao.getSavedPlaylists().isEmpty())
        assertEquals(listOf("orphan.img"), covers.deletedNames)
    }

    @Test
    fun cancelledCoverDownloadPropagatesAndDoesNotWritePlaylist() = runTest {
        val dao = FakePlaylistTracksDao()
        val covers = FakePlaylistCoverStorage().apply {
            onSave = { throw CancellationException("cancel download") }
        }
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)

        try {
            repository.savePlaylist(
                PlaylistContent("Cancelled", emptyList(), "https://covers.test/cancel.jpg"),
            )
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertTrue(dao.getSavedPlaylists().isEmpty())
        }
    }

    @Test
    fun cancellationAfterDownloadBeforeDatabaseCommitDeletesNewFile() = runTest {
        val dao = FakePlaylistTracksDao()
        val covers = FakePlaylistCoverStorage().apply {
            onSave = {
                currentCoroutineContext().job.cancel(CancellationException("cancel before commit"))
                "downloaded.img"
            }
        }
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)
        val save = async {
            repository.savePlaylist(
                PlaylistContent("Cancelled after download", emptyList(), "https://covers.test/after.jpg"),
            )
        }

        try {
            save.await()
            fail("Cancellation must propagate before the database commit")
        } catch (_: CancellationException) {
            assertTrue(dao.getSavedPlaylists().isEmpty())
            assertEquals(listOf("downloaded.img"), covers.deletedNames)
        }
    }

    @Test
    fun firstLocalReadReconcilesCoverFilesAgainstDatabase() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(PlaylistEntity(id = 3L, title = "Saved", coverFileName = "kept.img"), emptyList()),
        )
        val covers = FakePlaylistCoverStorage()
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)

        repository.getLocalPlaylists().getOrThrow()

        assertEquals(listOf(setOf("kept.img")), covers.reconciledReferences)
    }

    @Test
    fun unknownStoredProviderMapsToNull() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(PlaylistEntity(id = 13L, title = "Legacy", provider = "FUTURE_PROVIDER"), emptyList()),
        )
        val repository = createRepository(MockEngine { error("No network") }, dao = dao)

        assertNull(repository.getLocalPlaylist(13L).getOrThrow().provider)
    }

    @Test
    fun failedReplacementKeepsOldMetadataAndDeletesOnlyNewFile() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(PlaylistEntity(id = 14L, title = "Original", coverFileName = "old.img"), emptyList()),
        )
        dao.failPlaylistWrite = true
        val covers = FakePlaylistCoverStorage(savedFileNames = ArrayDeque(listOf("new.img")))
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)

        val result = repository.replacePlaylist(
            14L,
            PlaylistContent("Replacement", emptyList(), "https://covers.test/new.jpg"),
            PlaylistProvider.YANDEX_MUSIC,
        )

        assertTrue(result.isFailure)
        assertEquals("Original", dao.getSavedPlaylist(14L)?.playlist?.title)
        assertEquals("old.img", dao.getSavedPlaylist(14L)?.playlist?.coverFileName)
        assertEquals(listOf("new.img"), covers.deletedNames)
    }

    @Test
    fun failedOldFileCleanupIsRetriedByNextRead() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(PlaylistEntity(id = 15L, title = "Before", coverFileName = "old.img"), emptyList()),
        )
        val covers = FakePlaylistCoverStorage(savedFileNames = ArrayDeque(listOf("new.img"))).apply {
            failDeleteNames += "old.img"
        }
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)

        repository.replacePlaylist(15L, PlaylistContent("After", emptyList(), "https://covers.test/new.jpg"))
            .getOrThrow()
        repository.getLocalPlaylists().getOrThrow()

        assertEquals(listOf(setOf("new.img")), covers.reconciledReferences.takeLast(1))
    }

    @Test
    fun saveAndReadAreSerializedWhileCoverDownloadIsSuspended() = runTest {
        val dao = FakePlaylistTracksDao()
        val downloadStarted = CompletableDeferred<Unit>()
        val finishDownload = CompletableDeferred<String?>()
        val covers = FakePlaylistCoverStorage().apply {
            onSave = {
                downloadStarted.complete(Unit)
                finishDownload.await()
            }
        }
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)

        val saving = async {
            repository.savePlaylist(PlaylistContent("Pending", emptyList(), "https://covers.test/wait.jpg"))
        }
        downloadStarted.await()
        val reading = async { repository.getLocalPlaylists() }
        runCurrent()
        assertEquals(0, dao.savedPlaylistReadCount)

        finishDownload.complete("pending.img")
        saving.await().getOrThrow()
        assertEquals(1, reading.await().getOrThrow().size)
        assertEquals(1, dao.savedPlaylistReadCount)
    }

    @Test
    fun replaceAndDeleteAreSerializedSoDeleteRemovesTheCommittedNewCover() = runTest {
        val dao = FakePlaylistTracksDao()
        dao.insertPlaylistWithTracks(
            PlaylistWithTracks(
                PlaylistEntity(id = 22L, title = "Before", coverFileName = "old.img"),
                emptyList(),
            ),
        )
        val downloadStarted = CompletableDeferred<Unit>()
        val finishDownload = CompletableDeferred<String?>()
        val covers = FakePlaylistCoverStorage().apply {
            onSave = {
                downloadStarted.complete(Unit)
                finishDownload.await()
            }
        }
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)
        val replacing = async {
            repository.replacePlaylist(
                22L,
                PlaylistContent("After", emptyList(), "https://covers.test/new.jpg"),
            )
        }

        downloadStarted.await()
        val deleting = async { repository.deletePlaylist(22L) }
        runCurrent()
        assertNotNull(dao.getSavedPlaylist(22L))
        assertTrue(covers.deletedNames.isEmpty())

        finishDownload.complete("new.img")
        replacing.await().getOrThrow()
        deleting.await().getOrThrow()

        assertNull(dao.getSavedPlaylist(22L))
        assertEquals(setOf("old.img", "new.img"), covers.deletedNames.toSet())
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
                playlist = PlaylistEntity(id = 7L, title = "To Delete", coverFileName = "delete-me.img"),
                tracks = listOf(TrackEntity(title = "Song", artist = "Artist")),
            ),
        )
        val covers = FakePlaylistCoverStorage()
        val repository = createRepository(MockEngine { error("No network") }, dao = dao, covers = covers)

        val result = repository.deletePlaylist(7L)

        assertTrue(result.isSuccess)
        assertNull(dao.getSavedPlaylist(7L))
        assertTrue(dao.getSavedPlaylists().isEmpty())
        assertEquals(listOf("delete-me.img"), covers.deletedNames)
    }

    private class FakePlaylistCoverStorage(
        private val savedFileNames: ArrayDeque<String> = ArrayDeque(),
    ) : PlaylistCoverStorage {
        val savedUrls = mutableListOf<String>()
        val deletedNames = mutableListOf<String>()
        val reconciledReferences = mutableListOf<Set<String>>()
        val failDeleteNames = mutableSetOf<String>()
        var onSave: suspend (String) -> String? = { savedFileNames.removeFirstOrNull() }

        override suspend fun save(url: String): String? {
            savedUrls += url
            return onSave(url)
        }

        override suspend fun delete(fileName: String) {
            if (fileName in failDeleteNames) error("simulated cleanup failure")
            deletedNames += fileName
        }

        override suspend fun reconcile(referencedFileNames: Set<String>) {
            reconciledReferences += referencedFileNames
        }
    }
}
