package org.mromichov.guessthesong.domain.repository

import org.mromichov.guessthesong.domain.model.TrackPreview

fun interface TrackArtworkRepository {
    suspend fun getCoverUrl(preview: TrackPreview): String?
}
