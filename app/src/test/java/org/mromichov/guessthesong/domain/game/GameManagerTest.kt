package org.mromichov.guessthesong.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mromichov.guessthesong.core.exception.ItunesAccessForbiddenException
import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewNotFoundException
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository

class GameManagerTest {
    @Test
    fun `preparation accepts inclusive snippet length boundaries`() = runTest {
        val manager = GameManager(FakePreviewRepository())

        manager.prepareRounds(playlist(tracks(4)), GameConfig(roundsNumber = 1, snippetLength = 1_000))
        manager.reset()
        manager.prepareRounds(playlist(tracks(40)), GameConfig(roundsNumber = 10, snippetLength = 30_000))
    }

    @Test
    fun `preparation rejects zero or negative rounds`() = runTest {
        val manager = GameManager(FakePreviewRepository())
        val playlist = playlist(tracks(4))

        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(0, 10_000))
        }
        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(-1, 10_000))
        }
    }

    @Test
    fun `preparation rejects snippet length below or above inclusive range`() = runTest {
        val manager = GameManager(FakePreviewRepository())
        val playlist = playlist(tracks(4))

        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(1, 0))
        }
        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(1, 999))
        }
        assertSuspendThrows(IllegalArgumentException::class.java) {
            manager.prepareRounds(playlist, GameConfig(1, 30_001))
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
    fun `prepared round choices are distinct by title and artist`() = runTest {
        val sourceTracks = tracks(4).flatMap { track ->
            listOf(track, track.copy(coverUrl = "cover://alternate", durationMs = 200_000))
        }
        val manager = GameManager(FakePreviewRepository())
        manager.prepareRounds(playlist(sourceTracks), GameConfig(1, 10_000))

        val answer = manager.getCurrentRound().track
        val choiceIdentities = manager.currentRoundOptions.map(::identity).toSet()

        assertEquals(4, choiceIdentities.size)
        assertTrue(identity(answer) in choiceIdentities)
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
        assertEquals(
            "Every round must have a distinct title and artist",
            answers.size,
            answers.map(::identity).distinct().size
        )
    }

    @Test
    fun `duplicate rich playlist prepares distinct questions and four distinct choices per round`() = runTest {
        val uniqueTracks = tracks(4)
        val sourceTracks = uniqueTracks.flatMap { track ->
            listOf(track, track.copy(coverUrl = "cover://alternate", durationMs = 200_000))
        }
        val manager = GameManager(FakePreviewRepository())
        manager.prepareRounds(playlist(sourceTracks), GameConfig(4, 10_000))

        val answers = mutableListOf<Track>()
        repeat(4) { roundIndex ->
            val round = manager.getCurrentRound()
            answers += round.track

            val choiceIdentities = manager.currentRoundOptions.map(::identity).toSet()
            assertEquals("Each round must expose four distinct title/artist pairs", 4, choiceIdentities.size)
            assertTrue(identity(round.track) in choiceIdentities)
            if (roundIndex < 3) manager.nextRound()
        }

        assertEquals(4, answers.map(::identity).distinct().size)
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
    fun `four distinct tracks fail when five questions are requested`() = runTest {
        assertInsufficientTracksFailBeforePreview(tracks(4), rounds = 5)
    }

    @Test
    fun `four copies of one track fail before preview lookup`() = runTest {
        assertInsufficientTracksFailBeforePreview(List(4) { tracks(1).single() }, rounds = 4)
    }

    @Test
    fun `three distinct tracks fail even for one round because four choices are required`() = runTest {
        assertInsufficientTracksFailBeforePreview(tracks(3), rounds = 1)
    }

    @Test
    fun `raw playlist size does not satisfy distinct question requirement`() = runTest {
        val sourceTracks = listOf(tracks(3)[0], tracks(3)[0], tracks(3)[1], tracks(3)[2])
        assertInsufficientTracksFailBeforePreview(sourceTracks, rounds = 4)
    }

    @Test
    fun `metadata variants of same title and artist count as one track`() = runTest {
        val sameIdentity = tracks(1).single()
        val variants =
            List(4) { index -> sameIdentity.copy(coverUrl = "cover://$index", durationMs = 180_000L + index) }
        assertInsufficientTracksFailBeforePreview(variants, rounds = 1)
    }

    @Test
    fun `preparation fails with track count error for an empty playlist`() = runTest {
        val manager = GameManager(FakePreviewRepository())

        assertSuspendThrows(TrackCountException::class.java) {
            manager.prepareRounds(playlist(emptyList()), GameConfig(1, 10_000))
        }
    }

    @Test
    fun `missing preview is skipped when enough other previews remain`() = runTest {
        val tracks = tracks(5)
        var lookupCount = 0
        val manager = GameManager(
            FakePreviewRepository { artist, title ->
                lookupCount++
                if (lookupCount == 1) {
                    Result.failure(TrackPreviewNotFoundException(title))
                } else {
                    Result.success(TrackPreview(title, artist, "preview://$artist/$title", null))
                }
            }
        )

        manager.prepareRounds(playlist(tracks), GameConfig(2, 10_000))

        val answers = mutableListOf(manager.getCurrentRound().track)
        manager.nextRound()
        answers += manager.getCurrentRound().track
        assertEquals(3, lookupCount)
        assertEquals(2, answers.size)
        assertEquals(2, answers.distinct().size)
    }

    @Test
    fun `too few found previews reports actual and needed counts`() = runTest {
        val tracks = tracks(4)
        val manager = GameManager(
            FakePreviewRepository { _, title ->
                if (title == tracks.first().title) {
                    Result.failure(TrackPreviewNotFoundException(title))
                } else {
                    null
                }
            }
        )

        val error = assertSuspendThrows(TrackPreviewCountException::class.java) {
            manager.prepareRounds(playlist(tracks), GameConfig(4, 10_000))
        }

        assertEquals("There are found 3 track previews. Needed: 4", error.message)
    }

    @Test
    fun `unexpected preview lookup failure propagates unchanged`() = runTest {
        val failure = IllegalStateException("preview service unavailable")
        val manager = GameManager(FakePreviewRepository { _, _ -> Result.failure(failure) })

        val actual = assertSuspendThrows(IllegalStateException::class.java) {
            manager.prepareRounds(playlist(tracks(4)), GameConfig(1, 10_000))
        }

        assertSame(failure, actual)
    }

    @Test
    fun `iTunes forbidden failure propagates immediately without trying remaining tracks`() = runTest {
        val forbidden = ItunesAccessForbiddenException()
        var lookupCount = 0
        val manager = GameManager(FakePreviewRepository { _, _ ->
            lookupCount++
            Result.failure(forbidden)
        })

        val actual = assertSuspendThrows(ItunesAccessForbiddenException::class.java) {
            manager.prepareRounds(playlist(tracks(4)), GameConfig(1, 10_000))
        }

        assertSame(forbidden, actual)
        assertEquals(1, lookupCount)
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

    private fun playlist(tracks: List<Track>) = PlaylistContent("Playlist", tracks)

    private fun identity(track: Track) = track.title to track.artist

    private suspend fun assertInsufficientTracksFailBeforePreview(tracks: List<Track>, rounds: Int) {
        val previewRepository = FailFastPreviewRepository()
        val manager = GameManager(previewRepository)

        assertSuspendThrows(TrackCountException::class.java) {
            manager.prepareRounds(playlist(tracks), GameConfig(rounds, 10_000))
        }
        assertEquals("Invalid playlists must be rejected before preview lookup", 0, previewRepository.lookupCount)
    }

    private class FakePreviewRepository(
        private val resultForTrack: (artist: String, title: String) -> Result<TrackPreview>? = { _, _ -> null },
    ) : TrackPreviewRepository {
        override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> =
            resultForTrack(artist, title)
                ?: Result.success(TrackPreview(title, artist, "preview://$artist/$title", null))
    }

    private class FailFastPreviewRepository : TrackPreviewRepository {
        var lookupCount = 0
            private set

        override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> {
            lookupCount++
            throw AssertionError("Preview lookup must not happen for an invalid playlist")
        }
    }
}
