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
            trackName = dto.trackName.orEmpty(),
            artistName = dto.artistName.orEmpty(),
            previewUrl = dto.previewUrl.orEmpty(),
            artworkUrl = dto.artworkUrl100
        )
    }
}
