package org.mromichov.guessthesong.presentation.startinput

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.core.theme.GuessTheSongTheme
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import org.mromichov.guessthesong.domain.model.playlist.PlaylistSource
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository

@RunWith(AndroidJUnit4::class)
class StartInputScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var viewModel: StartInputViewModel

    @Test
    fun menuShowsImportAndSavedPlaylistActionsWithoutUrlField() {
        showScreen(FakePlaylistRepository())

        composeRule.onNodeWithText("Загрузить плейлист").assertIsDisplayed()
        composeRule.onNodeWithText("Сохранённые плейлисты").assertIsDisplayed()
        composeRule.onNodeWithText("Ссылка на плейлист").assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun openingAndCancelingImportFormReturnsToMenu() {
        showScreen(FakePlaylistRepository())

        composeRule.onNodeWithText("Загрузить плейлист").performClick()
        composeRule.onNodeWithText("Ссылка на плейлист").assertIsDisplayed()
        composeRule.onNodeWithText("Отмена").performClick()

        assertMenu()
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun backFromImportFormReturnsToMenu() {
        showScreen(FakePlaylistRepository())
        composeRule.onNodeWithText("Загрузить плейлист").performClick()
        composeRule.onNodeWithText("Ссылка на плейлист").assertExists()

        pressBack()

        assertMenu()
    }

    @Test
    fun importUrlDraftSurvivesComposeSavedStateRestore() {
        val url = "https://example.test/saved-draft"
        val stateRestorationTester = StateRestorationTester(composeRule)
        viewModel = StartInputViewModel(
            playlistRepository = FakePlaylistRepository(),
            gameManager = GameManager(ImmediatePreviewRepository()),
            settingsRepository = TestSettingsRepository(),
        )
        stateRestorationTester.setContent {
            GuessTheSongTheme {
                StartInputScreen(
                    viewModel = viewModel,
                    startGame = {},
                    onOpenSettings = {},
                    onOpenPlaylists = {},
                )
            }
        }
        composeRule.onNodeWithText("Загрузить плейлист").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(url)

        stateRestorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNode(hasSetTextAction()).assertTextEquals(url)
    }

    @Test
    fun successfulImportShowsSelectionAndExplicitStartWithoutAutoNavigation() {
        val content = playlist("Imported favorites")
        val repository = FakePlaylistRepository(
            fetch = {
                Result.success(remotePlaylist(content))
            },
            save = { imported, _ -> Result.success(LocalPlaylist(12L, imported)) },
        )
        var startCalls = 0
        showScreen(repository, onStartGame = { startCalls++ })

        openImportAndEnterUrl()
        composeRule.onNodeWithText("Загрузить").performClick()
        waitForState { screenStateIs<StartInputUiState.Selected>() }

        composeRule.onNodeWithText("Imported favorites").assertIsDisplayed()
        composeRule.onNodeWithText("Начать игру").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, startCalls) }
    }

    @Test
    fun backDuringPendingImportCancelsRequestAndIgnoresLateResult() {
        val content = playlist("Late import")
        val pendingFetch = CompletableDeferred<Result<RemotePlaylist>>()
        val repository = FakePlaylistRepository(fetch = { pendingFetch.await() })
        showScreen(repository)
        openImportAndEnterUrl()
        composeRule.onNode(indeterminateProgressBar()).assertDoesNotExist()
        composeRule.onNodeWithText("Загрузить").performClick()
        waitForState { screenStateIs<StartInputUiState.Importing>() }
        composeRule.onNode(indeterminateProgressBar()).assertIsDisplayed()

        pressBack()
        waitForState { screenStateIs<StartInputUiState.Menu>() }
        composeRule.onNode(indeterminateProgressBar()).assertDoesNotExist()
        pendingFetch.complete(Result.success(remotePlaylist(content)))
        composeRule.waitForIdle()

        assertMenu()
        composeRule.runOnIdle { assertEquals(0, repository.saveCalls) }
    }

    @Test
    fun backDuringPreparationKeepsSelectionAndDoesNotNavigateWhenCancelledWorkReturns() {
        val content = playlist("Preparing mix", tracks = tracks(5))
        val previewEntered = CompletableDeferred<Unit>()
        val releasePreview = CompletableDeferred<Unit>()
        val previews = object : TrackPreviewRepository {
            override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> {
                previewEntered.complete(Unit)
                withContext(NonCancellable) { releasePreview.await() }
                return Result.success(TrackPreview(title, artist, "preview://track", null))
            }
        }
        var startCalls = 0
        showScreen(
            FakePlaylistRepository(fetch = { Result.success(remotePlaylist(content)) }),
            previews,
            onStartGame = { startCalls++ },
        )
        openImportAndEnterUrl()
        composeRule.onNodeWithText("Загрузить").performClick()
        waitForState { screenStateIs<StartInputUiState.Selected>() }
        composeRule.onNodeWithText("Начать игру").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { previewEntered.isCompleted }

        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("Подготавливаем игру… 0% (0/1)")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Подготавливаем игру… 0% (0/1)").assertIsDisplayed()
            pressBack()
            composeRule.onNodeWithText("Preparing mix").assertIsDisplayed()
            composeRule.onNodeWithText("Завершаем отмену…").assertIsDisplayed()
        } finally {
            releasePreview.complete(Unit)
        }
        waitForState { screenStateIs<StartInputUiState.Selected>() }

        composeRule.onNodeWithText("Начать игру").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, startCalls) }
    }

    @Test
    fun dismissingSavedPlaylistConflictReturnsToImportForm() {
        val incoming = playlist("Shared mix")
        val existing = LocalPlaylist(2L, playlist("Shared mix"))
        val repository = FakePlaylistRepository(
            fetch = { Result.success(remotePlaylist(incoming)) },
            findByTitle = { Result.success(existing) },
        )
        showScreen(repository)
        openImportAndEnterUrl()
        composeRule.onNodeWithText("Загрузить").performClick()
        waitForState { screenStateIs<StartInputUiState.PlaylistAlreadySavedConflict>() }

        composeRule.onNodeWithText("Плейлист Shared mix уже сохранён").assertIsDisplayed()
        // The dialog overlays the form, so select its dismiss action from the last matching label.
        composeRule.onAllNodesWithText("Отмена")[1].performClick()

        composeRule.onNodeWithText("Ссылка на плейлист").assertIsDisplayed()
        composeRule.onNodeWithText("Загрузить").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, repository.saveCalls) }
    }

    private fun showScreen(
        repository: FakePlaylistRepository,
        previews: TrackPreviewRepository = ImmediatePreviewRepository(),
        onStartGame: () -> Unit = {},
    ) {
        viewModel = StartInputViewModel(
            playlistRepository = repository,
            gameManager = GameManager(previews),
            settingsRepository = TestSettingsRepository(),
        )
        composeRule.setContent {
            GuessTheSongTheme {
                StartInputScreen(
                    viewModel = viewModel,
                    startGame = onStartGame,
                    onOpenSettings = {},
                    onOpenPlaylists = {},
                )
            }
        }
    }

    private fun openImportAndEnterUrl() {
        composeRule.onNodeWithText("Загрузить плейлист").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("https://example.test/playlist")
    }

    private fun assertMenu() {
        composeRule.onNodeWithText("Загрузить плейлист").assertIsDisplayed()
        composeRule.onNodeWithText("Сохранённые плейлисты").assertIsDisplayed()
    }

    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun indeterminateProgressBar() = SemanticsMatcher.expectValue(
        SemanticsProperties.ProgressBarRangeInfo,
        ProgressBarRangeInfo.Indeterminate,
    )

    private inline fun <reified T : StartInputUiState> screenStateIs(): Boolean =
        currentViewModelState is T

    private val currentViewModelState: StartInputUiState
        get() = viewModel.uiState.value

    private fun waitForState(predicate: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000, condition = predicate)
    }

    private fun playlist(title: String, tracks: List<Track> = emptyList()) = PlaylistContent(title, tracks)

    private fun tracks(count: Int) = (0 until count).map { index ->
        Track("track-$index", "artist-$index", null, 180_000L)
    }

    private fun remotePlaylist(content: PlaylistContent) = RemotePlaylist(
        PlaylistSource(PlaylistProvider.SPOTIFY, "remote-id"),
        content,
    )

    private class TestSettingsRepository : SettingsRepository {
        override val roundsNumber = flowOf(1)
        override val snippetLength = flowOf(10_000L)
        override suspend fun setRoundsNumber(roundsNumber: Int) = Unit
        override suspend fun setSnippetLength(ms: Long) = Unit
    }

    private class ImmediatePreviewRepository : TrackPreviewRepository {
        override suspend fun getPreview(artist: String, title: String) = Result.success(
            TrackPreview(title, artist, "preview://$title", null),
        )
    }

    private class FakePlaylistRepository(
        private val fetch: suspend (String) -> Result<RemotePlaylist> = {
            Result.failure(IllegalStateException("unexpected fetch"))
        },
        private val findByTitle: suspend (String) -> Result<LocalPlaylist?> = { Result.success(null) },
        private val save: suspend (PlaylistContent, PlaylistProvider?) -> Result<LocalPlaylist> = { content, _ ->
            Result.success(LocalPlaylist(1L, content))
        },
    ) : PlaylistRepository {
        var saveCalls = 0

        override suspend fun fetchPlaylistByUrl(url: String) = fetch(url)
        override suspend fun getLocalPlaylists(): Result<List<LocalPlaylist>> = Result.success(emptyList())
        override suspend fun getLocalPlaylist(id: Long): Result<LocalPlaylist> =
            Result.failure(NoSuchElementException("missing"))
        override suspend fun findLocalPlaylistByTitle(title: String) = findByTitle(title)
        override suspend fun savePlaylist(
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> {
            saveCalls++
            return save(playlistContent, provider)
        }
        override suspend fun replacePlaylist(
            existingId: Long,
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> = Result.success(LocalPlaylist(existingId, playlistContent, provider = provider))
        override suspend fun deletePlaylist(id: Long): Result<Unit> = Result.success(Unit)
    }
}
