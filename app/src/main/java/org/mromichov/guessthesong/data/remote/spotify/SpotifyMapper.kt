package org.mromichov.guessthesong.data.remote.spotify

import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyWebEmbedDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyWebTrackDto
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import org.mromichov.guessthesong.domain.model.playlist.PlaylistSource
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist
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

    fun toDomain(playlistExternalId: String, dto: SpotifyWebEmbedDto): RemotePlaylist {
        val entity = dto.props?.pageProps?.state?.data?.entity
            ?: throw NoSuchElementException("Invalid Spotify embed response: missing entity")

        val coverUrl = entity.coverArt?.sources?.firstOrNull()?.url
        val title = entity.name ?: entity.title ?: "Spotify Playlist"

        val tracks = entity.trackList.map { toDomain(dto = it, coverUrl = coverUrl) }

        val playlistSource = PlaylistSource(PlaylistProvider.SPOTIFY, playlistExternalId)
        val playlistContent = PlaylistContent(
            title = title,
            tracks = tracks
        )
        return RemotePlaylist(source = playlistSource, content = playlistContent)
    }

    fun toPreviewDomain(dto: SpotifyWebTrackDto, coverUrl: String? = null): TrackPreview? {
        val trackPreviewUrl = dto.audioPreview?.url
        if (trackPreviewUrl.isNullOrBlank()) return null

        return TrackPreview(
            title = dto.title,
            artist = dto.subtitle.orEmpty(),
            trackPreviewUrl = trackPreviewUrl,
            coverUrl = coverUrl
        )
    }
}
