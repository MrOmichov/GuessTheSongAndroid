package org.mromichov.guessthesong

import android.content.Context
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.core.database.PlaylistDatabase
import org.mromichov.guessthesong.core.database.PlaylistMigrations
import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.data.dao.PlaylistTracksDao
import org.mromichov.guessthesong.data.local.FilePlaylistCoverStorage
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.data.repository.PlaylistRepositoryImpl
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import org.mromichov.guessthesong.presentation.startinput.StartInputUiState
import org.mromichov.guessthesong.presentation.startinput.StartInputViewModel
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SavedPlaylistSelectionPersistenceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun selectingSavedPlaylistAfterDatabaseReopenUsesPersistedCompositionWithoutProviderRequests() = runBlocking {
        val suffix = System.nanoTime()
        val databaseName = "saved-playlist-selection-$suffix.db"
        val coverDirectory = File(context.cacheDir, "saved-playlist-selection-$suffix")
        val requestCount = AtomicInteger()
        val client = HttpClient(MockEngine {
            requestCount.incrementAndGet()
            respond(content = "unexpected provider request", status = HttpStatusCode.OK)
        })
        var database: PlaylistDatabase? = openDatabase(databaseName)

        val savedContent = PlaylistContent(
            title = "Offline playlist",
            tracks = listOf(
                Track("Song One", "Artist One", coverUrl = null, durationMs = 0L),
                Track("Song Two", "Artist Two", coverUrl = null, durationMs = 0L),
                Track("Song Three", "Artist Three", coverUrl = null, durationMs = 0L),
            ),
        )

        try {
            val firstRepository = repository(requireNotNull(database), client, coverDirectory)
            val saved = firstRepository.savePlaylist(savedContent, PlaylistProvider.SPOTIFY).getOrThrow()
            assertEquals(0, requestCount.get())

            requireNotNull(database).close()
            database = null

            database = openDatabase(databaseName)
            val countingDao = CountingPlaylistTracksDao(requireNotNull(database).playlistTracksDao())
            val reopenedRepository = repository(requireNotNull(database), client, coverDirectory, countingDao)
            val viewModel = StartInputViewModel(
                playlistRepository = reopenedRepository,
                gameManager = GameManager(object : TrackPreviewRepository {
                    override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> =
                        error("Selecting a saved playlist must not prepare previews")
                }),
                settingsRepository = object : SettingsRepository {
                    override val roundsNumber: Flow<Int> = flowOf(5)
                    override val snippetLength: Flow<Long> = flowOf(30_000L)

                    override suspend fun setRoundsNumber(roundsNumber: Int) = Unit
                    override suspend fun setSnippetLength(ms: Long) = Unit
                },
            )

            viewModel.selectSavedPlaylist(saved.id)
            val selected = withTimeout(5_000) {
                viewModel.uiState.first { it is StartInputUiState.Selected }
            } as StartInputUiState.Selected

            assertEquals(savedContent.title, selected.playlist.title)
            assertEquals(savedContent.tracks.size, selected.playlist.tracks.size)
            assertEquals(trackIdentities(savedContent), trackIdentities(selected.playlist))
            val persistedPlaylists = reopenedRepository.getLocalPlaylists().getOrThrow()
            assertEquals(1, persistedPlaylists.size)
            assertEquals(saved.id, persistedPlaylists.single().id)
            assertEquals(savedContent.tracks.size, persistedPlaylists.single().content.tracks.size)
            assertEquals(trackIdentities(savedContent), trackIdentities(persistedPlaylists.single().content))
            assertEquals("Local selection must not fetch from Spotify, Yandex, or any network", 0, requestCount.get())
            assertEquals("Local selection must not save a playlist", 0, countingDao.insertPlaylistCount)
            assertEquals("Local selection must not replace a playlist", 0, countingDao.replacePlaylistCount)
        } finally {
            database?.close()
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
        coverDirectory: File,
        playlistTracksDao: PlaylistTracksDao = database.playlistTracksDao(),
    ) = PlaylistRepositoryImpl(
        yandexApi = YandexApi(client),
        yandexUrlParser = YandexPlaylistUrlParser(),
        yandexMapper = YandexMapper(),
        spotifyApi = SpotifyApi(client),
        spotifyUrlParser = SpotifyPlaylistUrlParser(),
        spotifyMapper = SpotifyMapper(),
        playlistTracksDao = playlistTracksDao,
        coverStorage = FilePlaylistCoverStorage(coverDirectory, client) { true },
    )

    private fun trackIdentities(content: PlaylistContent): Set<Pair<String, String>> =
        content.tracks.mapTo(mutableSetOf()) { it.title to it.artist }

    private class CountingPlaylistTracksDao(
        private val delegate: PlaylistTracksDao,
    ) : PlaylistTracksDao by delegate {
        var insertPlaylistCount = 0
            private set
        var replacePlaylistCount = 0
            private set

        override suspend fun insertPlaylistWithTracks(playlistWithTracks: PlaylistWithTracks): Long {
            insertPlaylistCount++
            return delegate.insertPlaylistWithTracks(playlistWithTracks)
        }

        override suspend fun replacePlaylistWithTracks(playlistWithTracks: PlaylistWithTracks) {
            replacePlaylistCount++
            delegate.replacePlaylistWithTracks(playlistWithTracks)
        }
    }
}
