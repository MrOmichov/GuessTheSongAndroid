package org.mromichov.guessthesong.data.remote.itunes

import org.mromichov.guessthesong.domain.model.TrackPreview
import javax.inject.Inject

class ItunesMapper @Inject constructor() {

    fun findBestMatch(response: ItunesResponseDto): TrackPreview? {
        val match = response.results.firstOrNull { !it.previewUrl.isNullOrBlank() } ?: return null
        return toDomain(match)
    }

    fun toDomain(dto: ItunesTrackDto): TrackPreview {
        return TrackPreview(
            title = dto.trackName.orEmpty(),
            artist = dto.artistName.orEmpty(),
            trackPreviewUrl = dto.previewUrl.orEmpty(),
            coverUrl = dto.artworkUrl100
        )
    }
}
