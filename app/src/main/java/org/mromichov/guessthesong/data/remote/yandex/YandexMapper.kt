package org.mromichov.guessthesong.data.remote.yandex

import org.mromichov.guessthesong.data.remote.yandex.dto.YandexApiPlaylistResponseDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexTrackDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexWebPlaylistResponseDto
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track
import javax.inject.Inject

class YandexMapper @Inject constructor() {

    fun toDomain(dto: YandexTrackDto): Track {
        val artistsFormatted = dto.artists.joinToString(", ") { it.name.trim() }.trim()
        val rawCover = dto.coverUri ?: dto.ogImage
        val coverUrl = rawCover?.let { formatCoverUrl(it) }

        return Track(
            id = dto.id.orEmpty(),
            title = dto.title,
            artist = artistsFormatted,
            coverUrl = coverUrl,
            durationMs = dto.durationMs
        )
    }

    fun toDomain(playlistId: String, dto: YandexWebPlaylistResponseDto): Playlist {
        val webPlaylist = dto.playlist
        val tracks = webPlaylist?.tracks.orEmpty().map { toDomain(it) }
        return Playlist(
            id = playlistId,
            title = webPlaylist?.title.orEmpty(),
            tracks = tracks
        )
    }

    fun toDomain(playlistId: String, dto: YandexApiPlaylistResponseDto): Playlist {
        val apiPlaylist = dto.result
        val tracks = apiPlaylist?.tracks.orEmpty()
            .mapNotNull { it.track }
            .map { toDomain(it) }
        return Playlist(
            id = playlistId,
            title = apiPlaylist?.title.orEmpty(),
            tracks = tracks
        )
    }

    private fun formatCoverUrl(uri: String): String {
        val resolved = uri.replace("%%", "400x400")
        return if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            resolved
        } else {
            "https://$resolved"
        }
    }
}
