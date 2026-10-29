package org.mromichov.guessthesong.presentation.game.artwork

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackArtworkRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** A strong reference to a decoded image, owned by the current round's UI state. */
interface PreparedCover

fun interface TrackCoverLoader {
    suspend fun load(url: String): PreparedCover?
}

class PrepareTrackCover @Inject constructor(
    private val repository: TrackArtworkRepository,
    private val loader: TrackCoverLoader,
) {
    suspend fun prepare(preview: TrackPreview): PreparedCover? = withTimeoutOrNull(5_000L.milliseconds) {
        try {
            repository.getCoverUrl(preview)?.let { loader.load(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Artwork is optional; a playable audio preview must remain usable.
            null
        }
    }
}
