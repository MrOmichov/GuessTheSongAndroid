package org.mromichov.guessthesong.domain.repository

import org.mromichov.guessthesong.domain.model.TrackPreview

interface TrackPreviewRepository {
    suspend fun getPreview(artist: String, title: String): Result<TrackPreview>
}
