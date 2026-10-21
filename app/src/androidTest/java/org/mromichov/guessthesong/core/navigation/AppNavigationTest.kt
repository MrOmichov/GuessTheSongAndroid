package org.mromichov.guessthesong.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.core.di.LocalViewModelProvider
import org.mromichov.guessthesong.core.theme.GuessTheSongTheme
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist
import org.mromichov.guessthesong.domain.player.AudioPlayer
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import org.mromichov.guessthesong.domain.repository.TrackArtworkRepository
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import org.mromichov.guessthesong.presentation.game.artwork.PrepareTrackCover
import org.mromichov.guessthesong.presentation.game.artwork.TrackCoverLoader
import org.mromichov.guessthesong.presentation.game.GameViewModel
import org.mromichov.guessthesong.presentation.playlists.PlaylistsViewModel
import org.mromichov.guessthesong.presentation.settings.SettingsViewModel
import org.mromichov.guessthesong.presentation.startinput.StartInputUiState
import org.mromichov.guessthesong.presentation.startinput.StartInputViewModel

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var factoryForWait: FakeViewModelFactory

    @Test
    fun savedPlaylistSelectionRequiresExplicitStartAndBackDoesNotRestartGame() {
        val local = LocalPlaylist(73L, playlist("Saved local mix"))
        val repository = FakePlaylistRepository(local)
        val previews = BlockingPreviewRepository()
        val player = CountingAudioPlayer()
        val factory = FakeViewModelFactory(repository, previews, player)
        factoryForWait = factory
        composeRule.setContent {
            GuessTheSongTheme {
                CompositionLocalProvider(LocalViewModelProvider provides factory) {
                    AppNavigation()
                }
            }
        }

        composeRule.onNodeWithText("Сохранённые плейлисты").performClick()
        composeRule.onNodeWithText("Saved local mix").assertIsDisplayed()
        composeRule.onNodeWithText("Saved local mix").performClick()
        waitForUiState<StartInputUiState.Selected>()

        composeRule.onNodeWithText("Saved local mix").assertIsDisplayed()
        composeRule.onNodeWithText("Начать игру").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf(73L), repository.localSelectionIds)
            assertEquals(0, player.playCalls)
        }

        // Holding preview preparation open lets both tap events hit one active preparation.
        composeRule.onNodeWithText("Начать игру").performTouchInput { doubleClick() }
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) { previews.firstRequest.isCompleted }
            composeRule.runOnIdle { assertEquals(1, previews.previewCalls) }
        } finally {
            previews.release.complete(Unit)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { player.playCalls == 1 }
        composeRule.onNodeWithText("0 / 10").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("Saved local mix").assertIsDisplayed()
        composeRule.onNodeWithText("Начать игру").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, previews.previewCalls)
            assertEquals(1, player.playCalls)
        }
    }

    private inline fun <reified T : StartInputUiState> waitForUiState() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            factoryForWait.startInputViewModel.uiState.value is T
        }
    }

    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun playlist(title: String) = PlaylistContent(
        title = title,
        tracks = (0 until 5).map { index ->
            Track("track-$index", "artist-$index", null, 180_000L)
        },
    )

    private class FakeViewModelFactory(
        private val repository: FakePlaylistRepository,
        previews: TrackPreviewRepository,
        private val audioPlayer: AudioPlayer,
    ) : ViewModelProvider.Factory {
        private val settings = TestSettingsRepository()
        private val gameManager = GameManager(previews)
        val startInputViewModel = StartInputViewModel(repository, gameManager, settings)
        private val playlistsViewModelFactory = { PlaylistsViewModel(repository) }

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel: ViewModel = when (modelClass) {
                StartInputViewModel::class.java -> startInputViewModel
                PlaylistsViewModel::class.java -> playlistsViewModelFactory()
                GameViewModel::class.java -> GameViewModel(
                    gameManager,
                    audioPlayer,
                    PrepareTrackCover(TrackArtworkRepository { null }, TrackCoverLoader { null }),
                )
                SettingsViewModel::class.java -> SettingsViewModel(settings)
                else -> throw IllegalArgumentException("No test ViewModel for ${modelClass.name}")
            }
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
    }

    private class FakePlaylistRepository(
        private val saved: LocalPlaylist,
    ) : PlaylistRepository {
        val localSelectionIds = mutableListOf<Long>()

        override suspend fun fetchPlaylistByUrl(url: String): Result<RemotePlaylist> =
            Result.failure(UnsupportedOperationException("No remote import in navigation test"))

        override suspend fun getLocalPlaylists(): Result<List<LocalPlaylist>> = Result.success(listOf(saved))

        override suspend fun getLocalPlaylist(id: Long): Result<LocalPlaylist> {
            localSelectionIds += id
            return if (id == saved.id) Result.success(saved)
            else Result.failure(NoSuchElementException("Playlist $id not found"))
        }

        override suspend fun findLocalPlaylistByTitle(title: String): Result<LocalPlaylist?> = Result.success(null)

        override suspend fun savePlaylist(
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> = Result.success(LocalPlaylist(1L, playlistContent, provider = provider))

        override suspend fun replacePlaylist(
            existingId: Long,
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> = Result.success(LocalPlaylist(existingId, playlistContent, provider = provider))

        override suspend fun deletePlaylist(id: Long): Result<Unit> = Result.success(Unit)
    }

    private class TestSettingsRepository : SettingsRepository {
        override val roundsNumber = flowOf(1)
        override val snippetLength = flowOf(10_000L)
        override suspend fun setRoundsNumber(roundsNumber: Int) = Unit
        override suspend fun setSnippetLength(ms: Long) = Unit
    }

    private class BlockingPreviewRepository : TrackPreviewRepository {
        val firstRequest = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var previewCalls = 0

        override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> {
            previewCalls++
            firstRequest.complete(Unit)
            release.await()
            return Result.success(TrackPreview(title, artist, "preview://$title", null))
        }
    }

    private class CountingAudioPlayer : AudioPlayer {
        private val playing = MutableStateFlow(false)
        private val finished = MutableStateFlow(false)
        private val playerError = MutableStateFlow<androidx.media3.common.PlaybackException?>(null)
        private val current = MutableStateFlow(0L)
        private val duration = MutableStateFlow(10_000L)
        var playCalls = 0

        override val isPlaying: StateFlow<Boolean> = playing
        override val snippetFinished: StateFlow<Boolean> = finished
        override val error: StateFlow<androidx.media3.common.PlaybackException?> = playerError
        override val currentMs: StateFlow<Long> = current
        override val durationMs: StateFlow<Long> = duration

        override fun play(url: String, startMs: Long, endMs: Long) {
            playCalls++
            playing.value = true
        }

        override fun stop() {
            playing.value = false
        }

        override fun release() = Unit
    }
}
