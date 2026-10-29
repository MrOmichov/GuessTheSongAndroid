package org.mromichov.guessthesong.presentation.game

import androidx.lifecycle.ViewModelStore
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mromichov.guessthesong.domain.game.GameConfig
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.player.AudioPlayer
import org.mromichov.guessthesong.domain.repository.TrackArtworkRepository
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import org.mromichov.guessthesong.presentation.game.artwork.PrepareTrackCover
import org.mromichov.guessthesong.presentation.game.artwork.PreparedCover
import org.mromichov.guessthesong.presentation.game.artwork.TrackCoverLoader

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `cover preparation completes before audio and duplicate starts are ignored`() = runTest(dispatcher.scheduler) {
        val harness = harness(rounds = 1)
        val loaderStarted = CompletableDeferred<Unit>()
        val releaseLoader = CompletableDeferred<Unit>()
        val image = object : PreparedCover {}
        val order = mutableListOf<String>()
        val viewModel = harness.viewModel(
            order = order,
            cover = image,
            loader = TrackCoverLoader {
                loaderStarted.complete(Unit)
                releaseLoader.await()
                order += "cover"
                image
            },
        )

        runCurrent()
        viewModel.startRoundOrGameOver()
        runCurrent()

        assertTrue(loaderStarted.isCompleted)
        assertEquals(0, harness.player.playUrls.size)
        releaseLoader.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("cover", "play"), order)
        assertSame(image, (viewModel.uiState.value as GameUiState.Guessing).preparedCover)
        assertEquals(1, harness.player.playUrls.size)
    }

    @Test
    fun `cover is retained through answer and second round gets its own cover`() = runTest(dispatcher.scheduler) {
        val harness = harness(rounds = 2)
        val firstCover = object : PreparedCover {}
        val secondCover = object : PreparedCover {}
        var call = 0
        val viewModel = harness.viewModel(
            loader = TrackCoverLoader {
                if (call++ == 0) firstCover else secondCover
            },
            cover = firstCover,
        )

        advanceUntilIdle()
        val firstAnswer = harness.manager.getCurrentRound().track
        viewModel.checkAnswer(firstAnswer)
        val answer = viewModel.uiState.value as GameUiState.Answer
        assertSame(firstCover, answer.preparedCover)

        viewModel.startRoundOrGameOver()
        assertNull((viewModel.uiState.value as GameUiState.ListeningPreview).preparedCover)
        advanceUntilIdle()
        val guessing = viewModel.uiState.value as GameUiState.Guessing
        assertEquals(1, guessing.currentRoundIndex)
        assertSame(secondCover, guessing.preparedCover)
        assertEquals(2, harness.player.playUrls.size)
    }

    @Test
    fun `viewmodel clearing cancels preparation and prevents stale audio playback`() = runTest(dispatcher.scheduler) {
        val harness = harness(rounds = 1)
        val loaderStarted = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val viewModel = harness.viewModel(
            loader = TrackCoverLoader {
                loaderStarted.complete(Unit)
                try {
                    CompletableDeferred<Unit>().await()
                } finally {
                    cancelled.complete(Unit)
                }
                null
            },
        )
        val store = ViewModelStore().apply { put("game", viewModel) }

        runCurrent()
        assertTrue(loaderStarted.isCompleted)
        store.clear()
        runCurrent()

        assertTrue(cancelled.isCompleted)
        assertTrue(harness.player.playUrls.isEmpty())
    }

    @Test
    fun `image failure and five second timeout both continue to audio without cover`() = runTest(dispatcher.scheduler) {
        val failed = harness(rounds = 1).let { harness ->
            val viewModel = harness.viewModel(loader = TrackCoverLoader { throw IllegalStateException("decode") })
            advanceUntilIdle()
            assertEquals(1, harness.player.playUrls.size)
            assertNull((viewModel.uiState.value as GameUiState.Guessing).preparedCover)
            viewModel
        }
        assertTrue(failed.uiState.value is GameUiState.Guessing)

        val timedOutHarness = harness(rounds = 1)
        val timedOut = timedOutHarness.viewModel(loader = TrackCoverLoader {
            delay(5_001)
            object : PreparedCover {}
        })
        runCurrent()
        advanceUntilIdle()

        assertEquals(1, timedOutHarness.player.playUrls.size)
        assertNull((timedOut.uiState.value as GameUiState.Guessing).preparedCover)
    }

    @Test
    fun `answer can be scored once and keeps the selected track`() = runTest(dispatcher.scheduler) {
        val harness = harness(rounds = 2)
        val viewModel = harness.viewModel()
        advanceUntilIdle()
        val currentAnswer = harness.manager.getCurrentRound().track

        viewModel.checkAnswer(currentAnswer)
        val answered = viewModel.uiState.value as GameUiState.Answer
        viewModel.checkAnswer(currentAnswer)

        assertEquals(answered, viewModel.uiState.value)
        assertEquals(currentAnswer, answered.answer)
        assertEquals(1, harness.manager.score)
        assertEquals(1, harness.manager.currentRoundIndex)
    }

    @Test
    fun `last answer transitions to game over with final score without replaying audio`() = runTest(dispatcher.scheduler) {
        val harness = harness(rounds = 2)
        val viewModel = harness.viewModel()
        advanceUntilIdle()

        viewModel.checkAnswer(harness.manager.getCurrentRound().track)
        viewModel.startRoundOrGameOver()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is GameUiState.Guessing)
        assertEquals(2, harness.player.playUrls.size)

        val secondRound = harness.manager.getCurrentRound()
        val wrongAnswer = secondRound.wrongOptions.first()
        viewModel.checkAnswer(wrongAnswer)
        viewModel.startRoundOrGameOver()

        assertEquals(GameUiState.GameOver(score = 1), viewModel.uiState.value)
        assertEquals(2, harness.player.playUrls.size)
    }

    @Test
    fun `replacement round during cover load prevents stale audio playback`() = runTest(dispatcher.scheduler) {
        val harness = harness(rounds = 1)
        val loaderStarted = CompletableDeferred<Unit>()
        val releaseLoader = CompletableDeferred<Unit>()
        val viewModel = harness.viewModel(loader = TrackCoverLoader {
            loaderStarted.complete(Unit)
            releaseLoader.await()
            object : PreparedCover {}
        })
        runCurrent()
        assertTrue(loaderStarted.isCompleted)

        harness.manager.reset()
        harness.manager.prepareRounds(
            PlaylistContent("replacement", (0 until 6).map { Track("new-$it", "artist-$it", null, 180_000L) }),
            GameConfig(1, 10_000L),
        )
        releaseLoader.complete(Unit)
        runCurrent()

        assertTrue(harness.player.playUrls.isEmpty())
        assertTrue(viewModel.uiState.value is GameUiState.ListeningPreview)
    }

    private suspend fun harness(rounds: Int): Harness {
        val player = RecordingAudioPlayer()
        val manager = GameManager(object : TrackPreviewRepository {
            override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> =
                Result.success(TrackPreview(title, artist, "audio://$title", null))
        })
        manager.prepareRounds(
            PlaylistContent("mix", (0 until 6).map { Track("title-$it", "artist-$it", null, 180_000L) }),
            GameConfig(rounds, 10_000L),
        )
        return Harness(manager, player)
    }

    private fun Harness.viewModel(
        order: MutableList<String> = mutableListOf(),
        cover: PreparedCover? = null,
        loader: TrackCoverLoader = TrackCoverLoader {
            order += "cover"
            cover
        },
    ) = GameViewModel(
        manager,
        player.also { it.onPlay = { order += "play" } },
        PrepareTrackCover(TrackArtworkRepository { "https://art.test/${it.title}.png" }, loader),
    )

    private data class Harness(val manager: GameManager, val player: RecordingAudioPlayer)

    private class RecordingAudioPlayer : AudioPlayer {
        private val playing = MutableStateFlow(false)
        private val finished = MutableStateFlow(false)
        private val playbackError = MutableStateFlow<PlaybackException?>(null)
        private val current = MutableStateFlow(0L)
        private val duration = MutableStateFlow(10_000L)
        val playUrls = mutableListOf<String>()
        var onPlay: () -> Unit = {}

        override val isPlaying: StateFlow<Boolean> = playing
        override val snippetFinished: StateFlow<Boolean> = finished
        override val error: StateFlow<PlaybackException?> = playbackError
        override val currentMs: StateFlow<Long> = current
        override val durationMs: StateFlow<Long> = duration

        override fun play(url: String, startMs: Long, endMs: Long) {
            playUrls += url
            onPlay()
            playing.value = false
            finished.value = true
        }

        override fun stop() { playing.value = false }
        override fun release() = Unit
    }
}
