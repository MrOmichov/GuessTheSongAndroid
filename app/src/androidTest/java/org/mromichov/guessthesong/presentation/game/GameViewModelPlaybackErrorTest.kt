package org.mromichov.guessthesong.presentation.game

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelStore
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.PlaybackException
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.domain.game.GameConfig
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.player.AudioPlayer
import org.mromichov.guessthesong.domain.repository.TrackArtworkRepository
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import org.mromichov.guessthesong.presentation.game.artwork.PrepareTrackCover
import org.mromichov.guessthesong.presentation.game.artwork.TrackCoverLoader

@RunWith(AndroidJUnit4::class)
class GameViewModelPlaybackErrorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun playerFailureShowsErrorInsteadOfGuessing() {
        val tracks = (0 until 5).map { Track("track-$it", "artist-$it", null, 180_000L) }
        val manager = GameManager(object : TrackPreviewRepository {
            override suspend fun getPreview(artist: String, title: String) =
                Result.success(TrackPreview(title, artist, "preview://$title", null))
        })
        runBlocking {
            manager.prepareRounds(PlaylistContent("mix", tracks), GameConfig(1, 10_000L))
        }

        val player = FailingAudioPlayer()
        val viewModelStore = ViewModelStore()
        lateinit var viewModel: GameViewModel
        composeRule.setContent { }
        composeRule.runOnIdle {
            viewModel = GameViewModel(
                manager,
                player,
                PrepareTrackCover(TrackArtworkRepository { null }, TrackCoverLoader { null }),
            )
            viewModelStore.put("game", viewModel)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value is GameUiState.Error
        }
        assertEquals(GameUiState.Error("player failed"), viewModel.uiState.value)
        assertFalse(viewModel.uiState.value is GameUiState.Guessing)
        assertEquals(1, player.playCalls)
        composeRule.runOnIdle { viewModelStore.clear() }
    }

    private class FailingAudioPlayer : AudioPlayer {
        private val playing = MutableStateFlow(false)
        private val finished = MutableStateFlow(false)
        private val playbackError = MutableStateFlow<PlaybackException?>(null)
        private val current = MutableStateFlow(0L)
        private val duration = MutableStateFlow(10_000L)
        var playCalls = 0

        override val isPlaying: StateFlow<Boolean> = playing
        override val snippetFinished: StateFlow<Boolean> = finished
        override val error: StateFlow<PlaybackException?> = playbackError
        override val currentMs: StateFlow<Long> = current
        override val durationMs: StateFlow<Long> = duration

        override fun play(url: String, startMs: Long, endMs: Long) {
            playCalls++
            playbackError.value = PlaybackException(
                "player failed",
                null,
                PlaybackException.ERROR_CODE_UNSPECIFIED,
            )
        }

        override fun stop() = Unit
        override fun release() = Unit
    }
}
