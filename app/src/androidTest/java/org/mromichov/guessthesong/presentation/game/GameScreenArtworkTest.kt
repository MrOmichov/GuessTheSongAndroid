package org.mromichov.guessthesong.presentation.game

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.PlaybackException
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.core.theme.GuessTheSongTheme
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
class GameScreenArtworkTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun questionCoverShowsWhileListeningAndGuessingThenAnswerShowsFallback() {
        val tracks = (0 until 5).map { Track("track-$it", "artist-$it", null, 180_000L) }
        val manager = GameManager(object : TrackPreviewRepository {
            override suspend fun getPreview(artist: String, title: String) =
                Result.success(TrackPreview(title, artist, "preview://$title", null))
        })
        runBlocking { manager.prepareRounds(PlaylistContent("mix", tracks), GameConfig(1, 10_000L)) }

        val player = ControlledAudioPlayer()
        val viewModelStore = ViewModelStore()
        lateinit var viewModel: GameViewModel
        composeRule.runOnIdle {
            viewModel = GameViewModel(
                manager,
                player,
                PrepareTrackCover(TrackArtworkRepository { null }, TrackCoverLoader { null }),
            )
            viewModelStore.put("game", viewModel)
        }
        composeRule.setContent {
            GuessTheSongTheme {
                GameScreen(viewModel, onEnd = {})
            }
        }

        composeRule.waitUntil(5_000) { player.playCalls == 1 }
        composeRule.onNodeWithContentDescription("Обложка скрыта").assertIsDisplayed()
        composeRule.onNodeWithText("?").assertIsDisplayed()

        composeRule.runOnIdle { player.finishSnippet() }
        composeRule.waitUntil(5_000) { viewModel.uiState.value is GameUiState.Guessing }

        composeRule.onNodeWithContentDescription("Обложка скрыта").assertIsDisplayed()
        composeRule.onNodeWithText("?").assertIsDisplayed()
        val optionTexts = tracks.map { "${it.title} от ${it.artist}" }
            .filter { text -> composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(4, optionTexts.size)

        composeRule.onNodeWithText(optionTexts.first()).performClick()
        composeRule.waitUntil(5_000) { viewModel.uiState.value is GameUiState.Answer }

        composeRule.onNodeWithContentDescription("Обложка скрыта").assertDoesNotExist()
        composeRule.onNodeWithText("?").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Обложка трека").assertIsDisplayed()
        composeRule.onNodeWithText("Правильный ответ:", substring = true).assertIsDisplayed()

        composeRule.runOnIdle { viewModelStore.clear() }
    }

    private class ControlledAudioPlayer : AudioPlayer {
        private val playing = MutableStateFlow(false)
        private val finished = MutableStateFlow(false)
        private val playbackError = MutableStateFlow<PlaybackException?>(null)
        private val current = MutableStateFlow(0L)
        private val duration = MutableStateFlow(10_000L)
        @Volatile var playCalls = 0
            private set

        override val isPlaying: StateFlow<Boolean> = playing
        override val snippetFinished: StateFlow<Boolean> = finished
        override val error: StateFlow<PlaybackException?> = playbackError
        override val currentMs: StateFlow<Long> = current
        override val durationMs: StateFlow<Long> = duration

        override fun play(url: String, startMs: Long, endMs: Long) {
            playCalls++
            finished.value = false
            playing.value = true
        }

        fun finishSnippet() {
            current.value = duration.value
            playing.value = false
            finished.value = true
        }

        override fun stop() { playing.value = false }
        override fun release() = Unit
    }
}
