package org.mromichov.guessthesong.data.repository

import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackArtworkRepository
import javax.inject.Inject

class ItunesTrackArtworkRepository @Inject constructor(
    private val api: ItunesApi,
) : TrackArtworkRepository {
    override suspend fun getCoverUrl(preview: TrackPreview): String? {
        preview.coverUrl?.takeIf(String::isNotBlank)?.let { return it }
        return api.searchTrackPreview("${preview.artist} ${preview.title}".trim())
            .results.firstOrNull { it.previewUrl == preview.trackPreviewUrl }
            ?.artworkUrl100?.takeIf(String::isNotBlank)
    }
}
