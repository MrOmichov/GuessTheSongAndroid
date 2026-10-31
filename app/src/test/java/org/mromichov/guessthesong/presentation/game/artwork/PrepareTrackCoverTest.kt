package org.mromichov.guessthesong.presentation.game.artwork

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackArtworkRepository

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PrepareTrackCoverTest {
    private val image = object : PreparedCover {}

    @Test
    fun `prepare uses cover already attached to selected preview`() = runTest {
        var loadedUrl: String? = null
        val prepare = PrepareTrackCover(
            repository = TrackArtworkRepository {
                it.coverUrl
            },
            loader = TrackCoverLoader { url ->
                loadedUrl = url
                image
            },
        )

        val result = prepare.prepare(preview(coverUrl = "https://art.test/matched.jpg"))

        assertSame(image, result)
        assertEquals("https://art.test/matched.jpg", loadedUrl)
    }

    @Test
    fun `prepare looks up artwork when cached preview has no cover URL`() = runTest {
        val selected = preview(coverUrl = null)
        var lookedUpPreview: TrackPreview? = null
        var loadedUrl: String? = null
        val prepare = PrepareTrackCover(
            repository = TrackArtworkRepository { preview ->
                lookedUpPreview = preview
                "https://art.test/cache-hit.jpg"
            },
            loader = TrackCoverLoader { url ->
                loadedUrl = url
                image
            },
        )

        assertSame(image, prepare.prepare(selected))
        assertEquals(selected, lookedUpPreview)
        assertEquals("https://art.test/cache-hit.jpg", loadedUrl)
    }

    @Test
    fun `prepare returns null if lookup finds no cover`() = runTest {
        var loaderCalled = false
        val prepare = PrepareTrackCover(
            repository = TrackArtworkRepository { null },
            loader = TrackCoverLoader {
                loaderCalled = true
                image
            },
        )

        assertNull(prepare.prepare(preview(coverUrl = null)))
        assertTrue(!loaderCalled)
    }

    @Test
    fun `prepare treats lookup and image failures as optional artwork`() = runTest {
        val lookupFailure = PrepareTrackCover(
            repository = TrackArtworkRepository { throw IllegalStateException("lookup") },
            loader = TrackCoverLoader { image },
        )
        val decodeFailure = PrepareTrackCover(
            repository = TrackArtworkRepository { "https://art.test/broken.jpg" },
            loader = TrackCoverLoader { throw IllegalArgumentException("decode") },
        )
        val noDecodedImage = PrepareTrackCover(
            repository = TrackArtworkRepository { "https://art.test/missing.jpg" },
            loader = TrackCoverLoader { null },
        )

        assertNull(lookupFailure.prepare(preview(coverUrl = null)))
        assertNull(decodeFailure.prepare(preview(coverUrl = "https://art.test/direct.jpg")))
        assertNull(noDecodedImage.prepare(preview(coverUrl = "https://art.test/direct.jpg")))
    }

    @Test
    fun `lookup and image load share five second budget`() = runTest {
        val loaderStarted = CompletableDeferred<Unit>()
        val prepare = PrepareTrackCover(
            repository = TrackArtworkRepository {
                delay(3_000)
                "https://art.test/slow.png"
            },
            loader = TrackCoverLoader {
                loaderStarted.complete(Unit)
                kotlinx.coroutines.awaitCancellation()
            },
        )

        val result = async { prepare.prepare(preview(coverUrl = null)) }
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertTrue(loaderStarted.isCompleted)
        advanceTimeBy(2_000)
        runCurrent()

        assertNull(result.await())
    }

    @Test
    fun `external cancellation propagates during preparation`() = runTest {
        val started = CompletableDeferred<Unit>()
        val prepare = PrepareTrackCover(
            repository = TrackArtworkRepository {
                started.complete(Unit)
                kotlinx.coroutines.awaitCancellation()
            },
            loader = TrackCoverLoader { image },
        )
        val job = async { prepare.prepare(preview(coverUrl = null)) }
        runCurrent()
        assertTrue(started.isCompleted)

        job.cancel(CancellationException("round changed"))
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
    }

    private fun preview(coverUrl: String?) = TrackPreview(
        title = "Song",
        artist = "Artist",
        trackPreviewUrl = "https://audio.test/song.m4a",
        coverUrl = coverUrl,
    )
}
