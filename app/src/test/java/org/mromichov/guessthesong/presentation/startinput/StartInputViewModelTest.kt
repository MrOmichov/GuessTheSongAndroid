package org.mromichov.guessthesong.presentation.startinput

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.playlist.*
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository

@OptIn(ExperimentalCoroutinesApi::class)
class StartInputViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `duplicate playlist can be replaced and exposes saved result`() = runTest(dispatcher.scheduler) {
        val imported = playlist("Mix")
        val saved = localPlaylist(9L, "Mix")
        val pendingReplace = CompletableDeferred<Result<LocalPlaylist>>()
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote", imported)),
            savedByTitle = Result.success(localPlaylist(9L, "Mix")),
            replace = { id, value ->
                assertEquals(9L, id)
                assertEquals(imported, value)
                pendingReplace.await()
            },
        )
        val viewModel = viewModel(repository)

        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()
        assertEquals(
            StartInputUiState.PlaylistAlreadySavedConflict(
                alreadyExistingPlaylist = localPlaylist(9L, "Mix"),
                fetchedPlaylist = remotePlaylist("remote", imported),
            ),
            viewModel.uiState.value,
        )
        assertEquals(0, repository.saveCalls)

        viewModel.replacePlaylist(9L, imported)
        runCurrent()
        assertSame(StartInputUiState.Loading, viewModel.uiState.value)

        viewModel.replacePlaylist(9L, imported)
        viewModel.addPlaylist(imported)
        viewModel.dismissPlaylistConflict()
        runCurrent()
        assertSame(StartInputUiState.Loading, viewModel.uiState.value)
        assertEquals(1, repository.replaceCalls)
        assertEquals(0, repository.saveCalls)
        assertEquals(PlaylistProvider.SPOTIFY, repository.lastReplacedProvider)

        pendingReplace.complete(Result.success(saved))
        advanceUntilIdle()

        assertEquals(StartInputUiState.Loaded(saved.content), viewModel.uiState.value)
        assertEquals(1, repository.replaceCalls)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `failed replacement exposes error`() = runTest(dispatcher.scheduler) {
        val imported = playlist("Mix")
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote", imported)),
            savedByTitle = Result.success(localPlaylist(9L, "Mix")),
            replace = { _, _ -> Result.failure(IllegalStateException("replace failed")) },
        )
        val viewModel = viewModel(repository)
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        viewModel.replacePlaylist(9L, imported)
        advanceUntilIdle()

        assertEquals(StartInputUiState.Error("replace failed"), viewModel.uiState.value)
    }

    @Test
    fun `duplicate playlist can be added separately and exposes saved result`() = runTest(dispatcher.scheduler) {
        val imported = playlist("Mix")
        val saved = localPlaylist(34L, "Mix (2)")
        val pendingSave = CompletableDeferred<Result<LocalPlaylist>>()
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote", imported)),
            savedByTitle = Result.success(localPlaylist(9L, "Mix")),
            save = { pendingSave.await() },
        )
        val viewModel = viewModel(repository)
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        viewModel.addPlaylist(imported)
        runCurrent()
        assertSame(StartInputUiState.Loading, viewModel.uiState.value)
        pendingSave.complete(Result.success(saved))
        advanceUntilIdle()

        assertEquals(StartInputUiState.Loaded(saved.content), viewModel.uiState.value)
        assertEquals(1, repository.saveCalls)
        assertEquals(PlaylistProvider.SPOTIFY, repository.lastSavedProvider)
        assertEquals(0, repository.replaceCalls)
    }

    @Test
    fun `dismissing saved playlist conflict returns to idle`() = runTest(dispatcher.scheduler) {
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote", playlist("Mix"))),
            savedByTitle = Result.success(localPlaylist(9L, "Mix")),
        )
        val viewModel = viewModel(repository)
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        viewModel.dismissPlaylistConflict()

        assertSame(StartInputUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `replace callback with stale playlist id is ignored`() = runTest(dispatcher.scheduler) {
        val imported = playlist("Mix")
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote", imported)),
            savedByTitle = Result.success(localPlaylist(9L, "Mix")),
        )
        val viewModel = viewModel(repository)
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        viewModel.replacePlaylist(10L, imported)
        advanceUntilIdle()

        assertEquals(
            StartInputUiState.PlaylistAlreadySavedConflict(
                alreadyExistingPlaylist = localPlaylist(9L, "Mix"),
                fetchedPlaylist = remotePlaylist("remote", imported),
            ),
            viewModel.uiState.value,
        )
        assertEquals(0, repository.replaceCalls)
    }

    @Test
    fun `saved playlist lookup failure exposes error without saving`() = runTest(dispatcher.scheduler) {
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote", playlist("Mix"))),
            savedByTitle = Result.failure(IllegalStateException("lookup failed")),
        )
        val viewModel = viewModel(repository)

        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        assertEquals(StartInputUiState.Error("lookup failed"), viewModel.uiState.value)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `fresh playlist exposes saved content`() = runTest(dispatcher.scheduler) {
        val imported = playlist("New mix")
        val saved = localPlaylist(42L, "New mix")
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote-id", imported, PlaylistProvider.YANDEX_MUSIC)),
            savedByTitle = Result.success(null),
            save = { Result.success(saved) },
        )
        val viewModel = viewModel(repository)

        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        assertEquals(StartInputUiState.Loaded(saved.content), viewModel.uiState.value)
        assertEquals(1, repository.saveCalls)
        assertEquals(PlaylistProvider.YANDEX_MUSIC, repository.lastSavedProvider)
    }

    @Test
    fun `fresh playlist save failure exposes error`() = runTest(dispatcher.scheduler) {
        val repository = FakePlaylistRepository(
            fetched = Result.success(remotePlaylist("remote-id", playlist("New mix"))),
            savedByTitle = Result.success(null),
            save = { Result.failure(IllegalStateException("save failed")) },
        )
        val viewModel = viewModel(repository)

        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        assertEquals(StartInputUiState.Error("save failed"), viewModel.uiState.value)
        assertEquals(1, repository.saveCalls)
    }

    private fun viewModel(repository: FakePlaylistRepository) = StartInputViewModel(
        playlistRepository = repository,
        gameManager = GameManager(object : TrackPreviewRepository {
            override suspend fun getPreview(artist: String, title: String) = error("Not used")
        }),
        settingsRepository = object : SettingsRepository {
            override val roundsNumber = flowOf(1)
            override val snippetLength = flowOf(10_000L)
            override suspend fun setRoundsNumber(roundsNumber: Int) = Unit
            override suspend fun setSnippetLength(ms: Long) = Unit
        },
    )

    private fun playlist(title: String) = PlaylistContent(title, emptyList())
    private fun localPlaylist(id: Long, title: String) = LocalPlaylist(id, playlist(title))
    private fun remotePlaylist(
        id: String,
        content: PlaylistContent,
        provider: PlaylistProvider = PlaylistProvider.SPOTIFY,
    ) = RemotePlaylist(
        PlaylistSource(provider, id), content,
    )

    private class FakePlaylistRepository(
        private val fetched: Result<RemotePlaylist>,
        private val savedByTitle: Result<LocalPlaylist?>,
        private val save: suspend (PlaylistContent) -> Result<LocalPlaylist> = {
            Result.success(
                LocalPlaylist(
                    1L,
                    it
                )
            )
        },
        private val replace: suspend (Long, PlaylistContent) -> Result<LocalPlaylist> = { id, playlist ->
            Result.success(
                LocalPlaylist(id, playlist)
            )
        },
    ) : PlaylistRepository {
        var saveCalls = 0
        var replaceCalls = 0
        var lastSavedProvider: PlaylistProvider? = null
        var lastReplacedProvider: PlaylistProvider? = null

        override suspend fun fetchPlaylistByUrl(url: String) = fetched
        override suspend fun getLocalPlaylists(): Result<List<LocalPlaylist>> = error("Not used")
        override suspend fun getLocalPlaylist(id: Long): Result<LocalPlaylist> = error("Not used")
        override suspend fun findLocalPlaylistByTitle(title: String) = savedByTitle
        override suspend fun savePlaylist(playlist: PlaylistContent, provider: PlaylistProvider?): Result<LocalPlaylist> {
            saveCalls++
            lastSavedProvider = provider
            return save(playlist)
        }

        override suspend fun replacePlaylist(existingId: Long, playlist: PlaylistContent, provider: PlaylistProvider?): Result<LocalPlaylist> {
            replaceCalls++
            lastReplacedProvider = provider
            return replace(existingId, playlist)
        }

        override suspend fun deletePlaylist(id: Long): Result<Unit> = error("Not used")
    }
}
