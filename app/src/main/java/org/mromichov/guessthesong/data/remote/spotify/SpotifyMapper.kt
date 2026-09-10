package org.mromichov.guessthesong.data.remote.spotify

import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyWebEmbedDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyWebTrackDto
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import javax.inject.Inject

class SpotifyMapper @Inject constructor() {

    fun toDomain(dto: SpotifyWebTrackDto, coverUrl: String? = null): Track {
        return Track(
            title = dto.title,
            artist = dto.subtitle.orEmpty(),
            coverUrl = coverUrl,
            durationMs = dto.duration
        )
    }

    fun toDomain(playlistId: String, dto: SpotifyWebEmbedDto): Playlist {
        val entity = dto.props?.pageProps?.state?.data?.entity
            ?: throw NoSuchElementException("Invalid Spotify embed response: missing entity")

        val coverUrl = entity.coverArt?.sources?.firstOrNull()?.url
        val title = entity.name ?: entity.title ?: "Spotify Playlist"

        val tracks = entity.trackList.map { toDomain(dto = it, coverUrl = coverUrl) }

        if (tracks.size < 20) throw TrackCountException()

        return Playlist(
            id = playlistId,
            title = title,
            tracks = tracks
        )
    }

    fun toPreviewDomain(dto: SpotifyWebTrackDto, artworkUrl: String? = null): TrackPreview? {
        val previewUrl = dto.audioPreview?.url
        if (previewUrl.isNullOrBlank()) return null

        return TrackPreview(
            trackName = dto.title,
            artistName = dto.subtitle.orEmpty(),
            previewUrl = previewUrl,
            artworkUrl = artworkUrl
        )
    }
}
