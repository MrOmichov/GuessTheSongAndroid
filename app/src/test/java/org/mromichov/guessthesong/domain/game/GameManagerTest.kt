package org.mromichov.guessthesong.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewException
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository

class GameManagerTest {
    @Test
    fun `preparation accepts positive rounds and snippet length inside supported range`() = runTest {
        val manager = GameManager(FakePreviewRepository())

        manager.prepareRounds(playlist(tracks(4)), GameConfig(roundsNumber = 1, snippetLength = 1))
        manager.reset()
        manager.prepareRounds(playlist(tracks(40)), GameConfig(roundsNumber = 10, snippetLength = 29_999))
    }

    @Test
    fun `preparation rejects zero or negative rounds`() = runTest {
        val manager = GameManager(FakePreviewRepository())
        val playlist = playlist(tracks(4))

        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(0, 1))
        }
        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(-1, 1))
        }
    }

    @Test
    fun `preparation rejects snippet length outside supported range`() = runTest {
        val manager = GameManager(FakePreviewRepository())
        val playlist = playlist(tracks(4))

        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(1, 0))
        }
        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(1, 30_000))
        }
        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(1, -1))
        }
    }

    @Test
    fun `prepared round exposes four distinct choices including the answer`() = runTest {
        val tracks = tracks(5)
        val manager = GameManager(FakePreviewRepository())
        manager.prepareRounds(playlist(tracks), GameConfig(1, 10_000))

        val round = manager.getCurrentRound()
        val choices = manager.currentRoundOptions
        assertEquals(4, choices.size)
        assertEquals(4, choices.distinct().size)
        assertTrue(round.track in choices)
        assertEquals(3, round.wrongOptions.size)
    }

    @Test
    fun `prepared rounds use a unique correct track for every question`() = runTest {
        val sourceTracks = tracks(13)
        val manager = GameManager(FakePreviewRepository())
        manager.prepareRounds(playlist(sourceTracks), GameConfig(10, 10_000))

        val answers = mutableListOf<Track>()
        repeat(10) { roundIndex ->
            answers += manager.getCurrentRound().track
            if (roundIndex < 9) manager.nextRound()
        }

        assertEquals(10, answers.size)
        assertEquals("Every round must have a distinct correct track", answers.size, answers.distinct().size)
    }

    @Test
    fun `correct answer increments score once and wrong answer does not`() = runTest {
        val tracks = tracks(5)
        val manager = GameManager(FakePreviewRepository())
        manager.prepareRounds(playlist(tracks), GameConfig(1, 10_000))
        val answer = manager.getCurrentRound().track
        val wrong = tracks.first { it != answer }

        assertFalse(manager.checkAnswer(wrong))
        assertEquals(0, manager.score)
        assertTrue(manager.checkAnswer(answer))
        assertEquals(1, manager.score)
    }

    @Test
    fun `preparation fails with track count error when fewer distinct questions than rounds`() = runTest {
        val manager = GameManager(FakePreviewRepository())

        assertSuspendThrows(TrackCountException::class.java) {
            manager.prepareRounds(playlist(tracks(4)), GameConfig(5, 10_000))
        }
    }

    @Test
    fun `preparation fails with track count error for an empty playlist`() = runTest {
        val manager = GameManager(FakePreviewRepository())

        assertSuspendThrows(TrackCountException::class.java) {
            manager.prepareRounds(playlist(emptyList()), GameConfig(1, 10_000))
        }
    }

    @Test
    fun `preview lookup failure is exposed as track preview exception`() = runTest {
        val manager = GameManager(FakePreviewRepository(fail = true))

        assertSuspendThrows(TrackPreviewException::class.java) {
            manager.prepareRounds(playlist(tracks(4)), GameConfig(1, 10_000))
        }
    }

    private suspend fun <T : Throwable> assertSuspendThrows(
        expectedType: Class<T>,
        block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (throwable: Throwable) {
            if (expectedType.isInstance(throwable)) return expectedType.cast(throwable)
            throw throwable
        }
        throw AssertionError("Expected ${expectedType.simpleName} to be thrown")
    }

    private fun tracks(count: Int) = (0 until count).map { index ->
        Track(title = "title-$index", artist = "artist-$index", coverUrl = null, durationMs = 180_000)
    }

    private fun playlist(tracks: List<Track>) = Playlist("playlist", "Playlist", tracks)

    private class FakePreviewRepository(private val fail: Boolean = false) : TrackPreviewRepository {
        override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> =
            if (fail) Result.failure(IllegalStateException("preview unavailable"))
            else Result.success(TrackPreview(title, artist, "preview://$artist/$title", null))
    }
}
