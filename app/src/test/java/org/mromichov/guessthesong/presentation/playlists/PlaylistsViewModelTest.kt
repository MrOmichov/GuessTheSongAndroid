package org.mromichov.guessthesong.presentation.playlists

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.repository.PlaylistRepository

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `initial load exposes exact saved playlists`() = runTest(mainDispatcher.scheduler) {
        val playlists = listOf(
            Playlist("1", "Favorites", listOf(Track("Song A", "Artist A", null, 120_000L))),
            Playlist("2", "Road trip", emptyList()),
        )
        val repository = FakePlaylistRepository { Result.success(playlists) }
        val viewModel = PlaylistsViewModel(repository)

        advanceUntilIdle()

        assertEquals(PlaylistsUiState.Loaded(playlists), viewModel.uiState.value)
        assertEquals(1, repository.savedPlaylistRequests)
    }

    @Test
    fun `empty saved playlists expose loaded empty state`() = runTest(mainDispatcher.scheduler) {
        val repository = FakePlaylistRepository { Result.success(emptyList()) }
        val viewModel = PlaylistsViewModel(repository)

        advanceUntilIdle()

        assertSame(PlaylistsUiState.LoadedEmpty, viewModel.uiState.value)
        assertEquals(1, repository.savedPlaylistRequests)
    }

    @Test
    fun `failed load can retry and reports loading while retry is suspended`() =
        runTest(mainDispatcher.scheduler) {
            val retryResult = CompletableDeferred<Result<List<Playlist>>>()
            var attempt = 0
            val playlists = listOf(Playlist("1", "Favorites", emptyList()))
            val repository = FakePlaylistRepository {
                if (attempt++ == 0) Result.failure(IllegalStateException("Database unavailable"))
                else retryResult.await()
            }
            val viewModel = PlaylistsViewModel(repository)

            runCurrent()
            assertEquals(PlaylistsUiState.Error("Database unavailable"), viewModel.uiState.value)

            viewModel.loadPlaylists()
            runCurrent()
            try {
                assertSame(PlaylistsUiState.Loading, viewModel.uiState.value)
            } finally {
                retryResult.complete(Result.success(playlists))
                advanceUntilIdle()
            }

            assertEquals(PlaylistsUiState.Loaded(playlists), viewModel.uiState.value)
            assertEquals(2, repository.savedPlaylistRequests)
        }

    @Test
    fun `second load while initial request is pending does not call repository again`() =
        runTest(mainDispatcher.scheduler) {
            val pendingResult = CompletableDeferred<Result<List<Playlist>>>()
            val repository = FakePlaylistRepository { pendingResult.await() }
            val viewModel = PlaylistsViewModel(repository)

            runCurrent()
            assertEquals(1, repository.savedPlaylistRequests)

            viewModel.loadPlaylists()
            runCurrent()
            try {
                assertEquals(1, repository.savedPlaylistRequests)
            } finally {
                pendingResult.complete(Result.success(emptyList()))
                advanceUntilIdle()
            }
        }

    private class FakePlaylistRepository(
        private val savedPlaylists: suspend () -> Result<List<Playlist>>,
    ) : PlaylistRepository {
        var savedPlaylistRequests: Int = 0
            private set

        override suspend fun getPlaylistByUrl(url: String): Result<Playlist> =
            error("Not used by PlaylistsViewModel")

        override suspend fun getSavedPlaylists(): Result<List<Playlist>> {
            savedPlaylistRequests++
            return savedPlaylists()
        }

        override suspend fun getSavedPlaylist(id: Long): Result<Playlist> =
            error("Not used by PlaylistsViewModel")
    }
}
