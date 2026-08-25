package org.mromichov.guessthesong.data.remote.spotify

import org.mromichov.guessthesong.core.exception.TrackCountException
import com.adamratzman.spotify.models.Playlist as SpotifyPlaylistDto
import com.adamratzman.spotify.models.PlaylistTrack as SpotifyPlaylistTrackDto
import com.adamratzman.spotify.models.Track as SpotifyTrackDto
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import javax.inject.Inject

class SpotifyMapper @Inject constructor() {

    fun toDomain(dto: SpotifyTrackDto): Track {
        val artistsFormatted = dto.artists.joinToString(", ") { it.name.orEmpty().trim() }.trim()
        val coverUrl = dto.album.images?.firstOrNull()?.url

        return Track(
            id = dto.id,
            title = dto.name,
            artist = artistsFormatted,
            coverUrl = coverUrl,
            durationMs = dto.durationMs.toLong()
        )
    }

    fun toDomain(playlistId: String, tracks: List<SpotifyPlaylistTrackDto>, title: String = "Spotify Playlist"): Playlist {
        val domainTracks = tracks.mapNotNull { playlistTrack ->
            val track = playlistTrack.track as? SpotifyTrackDto
            track?.let { toDomain(it) }
        }

        if (domainTracks.size < 20) throw TrackCountException()

        return Playlist(
            id = playlistId,
            title = title,
            tracks = domainTracks
        )
    }

    fun toDomain(dto: SpotifyPlaylistDto): Playlist {
        return toDomain(
            playlistId = dto.id,
            tracks = dto.tracks.items,
            title = dto.name
        )
    }

    fun toPreviewDomain(dto: SpotifyTrackDto): TrackPreview? {
        val previewUrl = dto.previewUrl
        if (previewUrl.isNullOrBlank()) return null

        val artistsFormatted = dto.artists.joinToString(", ") { it.name.orEmpty().trim() }.trim()
        val artworkUrl = dto.album.images?.firstOrNull()?.url

        return TrackPreview(
            trackId = null,
            trackName = dto.name,
            artistName = artistsFormatted,
            previewUrl = previewUrl,
            artworkUrl = artworkUrl
        )
    }
}
