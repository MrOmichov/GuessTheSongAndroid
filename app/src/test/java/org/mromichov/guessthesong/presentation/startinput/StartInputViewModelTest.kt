package org.mromichov.guessthesong.presentation.startinput

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mromichov.guessthesong.domain.game.GameConfig
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
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
    fun `import form loads and saves a fresh playlist`() = runTest(dispatcher.scheduler) {
        val content = playlist("New mix")
        val saved = localPlaylist(42L, content)
        val repository = FakePlaylistRepository(
            fetched = { Result.success(remotePlaylist("remote-id", content, PlaylistProvider.YANDEX_MUSIC)) },
            save = { Result.success(saved) },
        )
        val viewModel = viewModel(repository)

        viewModel.openImportForm()
        assertEquals(StartInputUiState.ImportForm, viewModel.uiState.value)
        viewModel.loadPlaylist("https://playlist")
        assertEquals(StartInputUiState.Importing, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(StartInputUiState.Selected(content), viewModel.uiState.value)
        assertEquals(1, repository.fetchCalls)
        assertEquals(1, repository.saveCalls)
        assertEquals(PlaylistProvider.YANDEX_MUSIC, repository.lastSavedProvider)
    }

    @Test
    fun `conflict replacement preserves provider and rejects stale callbacks`() = runTest(dispatcher.scheduler) {
        val incoming = playlist("Mix")
        val existing = localPlaylist(9L, playlist("Old mix"))
        val repository = FakePlaylistRepository(
            fetched = { Result.success(remotePlaylist("remote-id", incoming)) },
            findByTitle = { Result.success(existing) },
            replace = { _, content -> Result.success(localPlaylist(9L, content)) },
        )
        val viewModel = viewModel(repository)
        viewModel.openImportForm()
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        assertEquals(
            StartInputUiState.PlaylistAlreadySavedConflict(existing, remotePlaylist("remote-id", incoming)),
            viewModel.uiState.value,
        )
        viewModel.replacePlaylist(10L, incoming)
        viewModel.replacePlaylist(9L, playlist("stale content"))
        runCurrent()
        assertEquals(0, repository.replaceCalls)

        viewModel.replacePlaylist(9L, incoming)
        advanceUntilIdle()

        assertEquals(StartInputUiState.Selected(incoming), viewModel.uiState.value)
        assertEquals(1, repository.replaceCalls)
        assertEquals(0, repository.saveCalls)
        assertEquals(PlaylistProvider.SPOTIFY, repository.lastReplacedProvider)
    }

    @Test
    fun `conflict can be added as a separate saved playlist`() = runTest(dispatcher.scheduler) {
        val incoming = playlist("Mix")
        val repository = FakePlaylistRepository(
            fetched = { Result.success(remotePlaylist("remote-id", incoming)) },
            findByTitle = { Result.success(localPlaylist(9L, playlist("Existing mix"))) },
            save = { Result.success(localPlaylist(34L, it)) },
        )
        val viewModel = viewModel(repository)
        viewModel.openImportForm()
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        viewModel.addPlaylist(incoming)
        advanceUntilIdle()

        assertEquals(StartInputUiState.Selected(incoming), viewModel.uiState.value)
        assertEquals(1, repository.saveCalls)
        assertEquals(0, repository.replaceCalls)
        assertEquals(PlaylistProvider.SPOTIFY, repository.lastSavedProvider)
    }

    @Test
    fun `dismissing conflict returns to import form`() = runTest(dispatcher.scheduler) {
        val incoming = playlist("Mix")
        val repository = FakePlaylistRepository(
            fetched = { Result.success(remotePlaylist("remote-id", incoming)) },
            findByTitle = { Result.success(localPlaylist(9L, incoming)) },
        )
        val viewModel = viewModel(repository)
        viewModel.openImportForm()
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        viewModel.dismissPlaylistConflict()

        assertEquals(StartInputUiState.ImportForm, viewModel.uiState.value)
        assertEquals(0, repository.saveCalls)
        assertEquals(0, repository.replaceCalls)
    }

    @Test
    fun `selecting a saved playlist uses exact local content without remote or write calls`() = runTest(dispatcher.scheduler) {
        val saved = localPlaylist(17L, playlist("Local", tracks = tracks(6)))
        val repository = FakePlaylistRepository(
            fetched = { error("Local selection must not fetch") },
            getLocal = { id ->
                assertEquals(17L, id)
                Result.success(saved)
            },
        )
        val viewModel = viewModel(repository)

        viewModel.selectSavedPlaylist(saved.id)
        assertEquals(StartInputUiState.SelectingSavedPlaylist(saved.id), viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(StartInputUiState.Selected(saved.content), viewModel.uiState.value)
        assertEquals(1, repository.getLocalCalls)
        assertEquals(0, repository.fetchCalls)
        assertEquals(0, repository.saveCalls)
        assertEquals(0, repository.replaceCalls)
    }

    @Test
    fun `missing local playlist exposes selection error`() = runTest(dispatcher.scheduler) {
        val repository = FakePlaylistRepository(
            getLocal = { Result.failure(NoSuchElementException("playlist missing")) },
        )
        val viewModel = viewModel(repository)

        viewModel.selectSavedPlaylist(88L)
        advanceUntilIdle()

        assertEquals(StartInputUiState.SelectionError("playlist missing"), viewModel.uiState.value)
        assertEquals(0, repository.fetchCalls)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `duplicate local selection is ignored while first lookup is pending`() = runTest(dispatcher.scheduler) {
        val firstPlaylist = localPlaylist(17L, playlist("First local mix", tracks = tracks(5)))
        val pendingLookup = CompletableDeferred<Result<LocalPlaylist>>()
        val repository = FakePlaylistRepository(
            getLocal = { id ->
                assertEquals(17L, id)
                pendingLookup.await()
            },
        )
        val viewModel = viewModel(repository)

        viewModel.selectSavedPlaylist(17L)
        viewModel.selectSavedPlaylist(22L)
        runCurrent()

        assertEquals(StartInputUiState.SelectingSavedPlaylist(17L), viewModel.uiState.value)
        assertEquals(1, repository.getLocalCalls)

        pendingLookup.complete(Result.success(firstPlaylist))
        advanceUntilIdle()

        assertEquals(StartInputUiState.Selected(firstPlaylist.content), viewModel.uiState.value)
        assertEquals(1, repository.getLocalCalls)
        assertEquals(0, repository.fetchCalls)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `preparation failure retains selection and retry uses current settings after reset`() = runTest(dispatcher.scheduler) {
        val content = playlist("Retryable", tracks = tracks(7))
        val settings = MutableSettings(initialRounds = 2, initialSnippetMs = 10_000L)
        val previews = FakeTrackPreviewRepository()
        val manager = GameManager(previews)
        manager.prepareRounds(content, GameConfig(roundsNumber = 2, snippetLength = 10_000L))
        manager.checkAnswer(manager.getCurrentRound().track)
        manager.nextRound()
        previews.failuresRemaining = 1
        assertEquals(1, manager.currentRoundIndex)
        assertEquals(1, manager.score)

        val viewModel = viewModel(
            FakePlaylistRepository(getLocal = { Result.success(localPlaylist(3L, content)) }),
            manager,
            settings,
        )
        viewModel.selectSavedPlaylist(3L)
        advanceUntilIdle()
        assertEquals(StartInputUiState.Selected(content), viewModel.uiState.value)

        viewModel.prepareGame()
        advanceUntilIdle()

        assertEquals(StartInputUiState.PreparationError("preview unavailable", content), viewModel.uiState.value)
        assertEquals(0, manager.currentRoundIndex)
        assertEquals(0, manager.score)

        settings.roundsFlow.value = 3
        settings.snippetMsFlow.value = 12_000L
        viewModel.prepareGame()
        advanceUntilIdle()

        assertEquals(StartInputUiState.ReadyToPlay(content), viewModel.uiState.value)
        assertEquals(GameConfig(roundsNumber = 3, snippetLength = 12_000L), manager.config)
        assertEquals(0, manager.currentRoundIndex)
        assertEquals(0, manager.score)
    }

    @Test
    fun `ready navigation can be consumed only once`() = runTest(dispatcher.scheduler) {
        val content = playlist("Ready", tracks = tracks(5))
        val manager = GameManager(FakeTrackPreviewRepository())
        val viewModel = viewModel(
            FakePlaylistRepository(getLocal = { Result.success(localPlaylist(1L, content)) }),
            manager,
        )
        viewModel.selectSavedPlaylist(1L)
        advanceUntilIdle()
        viewModel.prepareGame()
        advanceUntilIdle()

        assertEquals(StartInputUiState.ReadyToPlay(content), viewModel.uiState.value)
        assertTrue(viewModel.consumeReadyToPlay())
        assertFalse(viewModel.consumeReadyToPlay())
    }

    @Test
    fun `change playlist explicitly clears current selection`() = runTest(dispatcher.scheduler) {
        val content = playlist("Selected", tracks = tracks(5))
        val viewModel = viewModel(
            FakePlaylistRepository(getLocal = { Result.success(localPlaylist(5L, content)) }),
        )
        viewModel.selectSavedPlaylist(5L)
        advanceUntilIdle()
        assertEquals(StartInputUiState.Selected(content), viewModel.uiState.value)

        viewModel.changePlaylist()

        assertEquals(StartInputUiState.Menu, viewModel.uiState.value)
    }

    @Test
    fun `duplicate import submissions do not start a second lookup`() = runTest(dispatcher.scheduler) {
        val lookup = CompletableDeferred<Result<RemotePlaylist>>()
        val repository = FakePlaylistRepository(fetched = { lookup.await() })
        val viewModel = viewModel(repository)
        viewModel.openImportForm()

        viewModel.loadPlaylist("https://playlist/first")
        viewModel.loadPlaylist("https://playlist/duplicate")
        runCurrent()

        assertEquals(1, repository.fetchCalls)
        assertEquals(StartInputUiState.Importing, viewModel.uiState.value)

        lookup.complete(Result.failure(IllegalStateException("offline")))
        advanceUntilIdle()
        assertEquals(StartInputUiState.ImportError("offline"), viewModel.uiState.value)
    }

    @Test
    fun `canceling lookup ignores late result and does not delete a committed save`() = runTest(dispatcher.scheduler) {
        val committedSaveReturn = CompletableDeferred<Unit>()
        val content = playlist("Committed")
        var committed = 0
        val repository = FakePlaylistRepository(
            fetched = { Result.success(remotePlaylist("remote", content)) },
            save = {
                committed++ // model durable commit before this suspending call returns
                committedSaveReturn.await()
                Result.success(localPlaylist(7L, it))
            },
        )
        val viewModel = viewModel(repository)
        viewModel.openImportForm()
        viewModel.loadPlaylist("https://playlist")
        runCurrent()
        assertEquals(1, committed)

        viewModel.cancelImportForm()
        assertEquals(StartInputUiState.CancellingToMenu, viewModel.uiState.value)
        runCurrent()
        committedSaveReturn.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, committed)
        assertEquals(0, repository.deleteCalls)
        assertEquals(StartInputUiState.Menu, viewModel.uiState.value)
    }

    @Test
    fun `canceling import lookup returns to menu and ignores its late result`() = runTest(dispatcher.scheduler) {
        val lookup = CompletableDeferred<Result<RemotePlaylist>>()
        val content = playlist("Late result")
        val repository = FakePlaylistRepository(fetched = { lookup.await() })
        val viewModel = viewModel(repository)
        viewModel.openImportForm()
        viewModel.loadPlaylist("https://playlist")
        runCurrent()

        viewModel.cancelImportForm()
        assertEquals(StartInputUiState.CancellingToMenu, viewModel.uiState.value)
        runCurrent()
        lookup.complete(Result.success(remotePlaylist("remote", content)))
        advanceUntilIdle()

        assertEquals(StartInputUiState.Menu, viewModel.uiState.value)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun `back handling reports whether it handled current flow state`() = runTest(dispatcher.scheduler) {
        val viewModel = viewModel(FakePlaylistRepository())

        assertFalse(viewModel.handleBack())
        viewModel.openImportForm()
        assertTrue(viewModel.handleBack())
        assertEquals(StartInputUiState.Menu, viewModel.uiState.value)
        assertFalse(viewModel.handleBack())
    }

    @Test
    fun `change during noncancellable preparation waits before another preparation starts`() = runTest(dispatcher.scheduler) {
        val firstContent = playlist("First", tracks = tracks(5, "first"))
        val secondContent = playlist("Second", tracks = tracks(5, "second"))
        val firstPreviewEntered = CompletableDeferred<Unit>()
        val releaseFirstPreview = CompletableDeferred<Unit>()
        var activePreviews = 0
        var maxActivePreviews = 0
        val previews = object : TrackPreviewRepository {
            override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> {
                activePreviews++
                maxActivePreviews = maxOf(maxActivePreviews, activePreviews)
                try {
                    if (title.startsWith("first")) {
                        firstPreviewEntered.complete(Unit)
                        withContext(NonCancellable) { releaseFirstPreview.await() }
                    }
                    return Result.success(TrackPreview(title, artist, "preview", null))
                } finally {
                    activePreviews--
                }
            }
        }
        val repository = FakePlaylistRepository(
            getLocal = { id ->
                Result.success(localPlaylist(id, if (id == 1L) firstContent else secondContent))
            },
        )
        val viewModel = viewModel(repository, GameManager(previews))

        viewModel.selectSavedPlaylist(1L)
        advanceUntilIdle()
        viewModel.prepareGame()
        runCurrent()
        firstPreviewEntered.await()

        assertTrue(viewModel.handleBack())
        runCurrent()
        assertEquals(StartInputUiState.CancellingPreparation(firstContent), viewModel.uiState.value)
        viewModel.prepareGame()
        runCurrent()
        assertEquals(1, maxActivePreviews)

        releaseFirstPreview.complete(Unit)
        advanceUntilIdle()
        assertEquals(StartInputUiState.Selected(firstContent), viewModel.uiState.value)

        viewModel.changePlaylist()
        viewModel.selectSavedPlaylist(2L)
        advanceUntilIdle()
        viewModel.prepareGame()
        advanceUntilIdle()
        assertTrue(maxActivePreviews <= 1)
    }

    @Test
    fun `change playlist cancels preparation and stale completion cannot restore ready state`() = runTest(dispatcher.scheduler) {
        val content = playlist("Preparing", tracks = tracks(5))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val previews = object : TrackPreviewRepository {
            override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
                return Result.success(TrackPreview(title, artist, "preview", null))
            }
        }
        val viewModel = viewModel(
            FakePlaylistRepository(getLocal = { Result.success(localPlaylist(1L, content)) }),
            GameManager(previews),
        )
        viewModel.selectSavedPlaylist(1L)
        advanceUntilIdle()
        viewModel.prepareGame()
        runCurrent()
        entered.await()

        assertTrue(viewModel.handleBack())
        runCurrent()
        assertEquals(StartInputUiState.CancellingPreparation(content), viewModel.uiState.value)
        release.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value is StartInputUiState.ReadyToPlay)
    }

    @Test
    fun `cancel exception remains cancellation and is not shown as import error`() = runTest(dispatcher.scheduler) {
        val repository = FakePlaylistRepository(
            fetched = { throw kotlinx.coroutines.CancellationException("cancelled") },
        )
        val viewModel = viewModel(repository)
        viewModel.openImportForm()
        viewModel.loadPlaylist("https://playlist")
        advanceUntilIdle()

        assertEquals(0, repository.saveCalls)
        assertFalse(viewModel.uiState.value is StartInputUiState.ImportError)
    }

    private fun viewModel(
        repository: FakePlaylistRepository,
        manager: GameManager = GameManager(FakeTrackPreviewRepository()),
        settings: MutableSettings = MutableSettings(),
    ) = StartInputViewModel(
        playlistRepository = repository,
        gameManager = manager,
        settingsRepository = settings,
    )

    private fun playlist(title: String, tracks: List<Track> = emptyList()) = PlaylistContent(title, tracks)

    private fun tracks(count: Int, prefix: String = "track") = (0 until count).map { index ->
        Track("$prefix-$index", "artist-$index", null, 180_000L)
    }

    private fun localPlaylist(id: Long, content: PlaylistContent) = LocalPlaylist(id, content)

    private fun remotePlaylist(
        id: String,
        content: PlaylistContent,
        provider: PlaylistProvider = PlaylistProvider.SPOTIFY,
    ) = RemotePlaylist(PlaylistSource(provider, id), content)

    private class MutableSettings(
        initialRounds: Int = 1,
        initialSnippetMs: Long = 10_000L,
    ) : SettingsRepository {
        val roundsFlow = MutableStateFlow(initialRounds)
        val snippetMsFlow = MutableStateFlow(initialSnippetMs)
        override val roundsNumber: Flow<Int> = roundsFlow
        override val snippetLength: Flow<Long> = snippetMsFlow
        override suspend fun setRoundsNumber(roundsNumber: Int) {
            roundsFlow.value = roundsNumber
        }
        override suspend fun setSnippetLength(ms: Long) {
            snippetMsFlow.value = ms
        }
    }

    private class FakeTrackPreviewRepository(
        var failuresRemaining: Int = 0,
    ) : TrackPreviewRepository {
        override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> {
            if (failuresRemaining > 0) {
                failuresRemaining--
                return Result.failure(IllegalStateException("preview unavailable"))
            }
            return Result.success(TrackPreview(title, artist, "preview://$title", null))
        }
    }

    private class FakePlaylistRepository(
        private val fetched: suspend (String) -> Result<RemotePlaylist> = {
            Result.failure(IllegalStateException("not configured"))
        },
        private val findByTitle: suspend (String) -> Result<LocalPlaylist?> = { Result.success(null) },
        private val getLocal: suspend (Long) -> Result<LocalPlaylist> = {
            Result.failure(NoSuchElementException("playlist missing"))
        },
        private val save: suspend (PlaylistContent) -> Result<LocalPlaylist> = {
            Result.success(LocalPlaylist(1L, it))
        },
        private val replace: suspend (Long, PlaylistContent) -> Result<LocalPlaylist> = { id, content ->
            Result.success(LocalPlaylist(id, content))
        },
    ) : PlaylistRepository {
        var fetchCalls = 0
        var getLocalCalls = 0
        var saveCalls = 0
        var replaceCalls = 0
        var deleteCalls = 0
        var lastSavedProvider: PlaylistProvider? = null
        var lastReplacedProvider: PlaylistProvider? = null

        override suspend fun fetchPlaylistByUrl(url: String): Result<RemotePlaylist> {
            fetchCalls++
            return fetched(url)
        }
        override suspend fun getLocalPlaylists(): Result<List<LocalPlaylist>> = Result.success(emptyList())
        override suspend fun getLocalPlaylist(id: Long): Result<LocalPlaylist> {
            getLocalCalls++
            return getLocal(id)
        }
        override suspend fun findLocalPlaylistByTitle(title: String): Result<LocalPlaylist?> = findByTitle(title)
        override suspend fun savePlaylist(
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> {
            saveCalls++
            lastSavedProvider = provider
            return save(playlistContent)
        }
        override suspend fun replacePlaylist(
            existingId: Long,
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> {
            replaceCalls++
            lastReplacedProvider = provider
            return replace(existingId, playlistContent)
        }
        override suspend fun deletePlaylist(id: Long): Result<Unit> {
            deleteCalls++
            return Result.success(Unit)
        }
    }
}
