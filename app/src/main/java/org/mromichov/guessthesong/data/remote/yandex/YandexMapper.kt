package org.mromichov.guessthesong.data.remote.yandex

import org.mromichov.guessthesong.data.remote.yandex.dto.YandexApiPlaylistResponseDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexTrackDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexWebPlaylistResponseDto
import org.mromichov.guessthesong.domain.model.PlaylistContent
import org.mromichov.guessthesong.domain.model.PlaylistProvider
import org.mromichov.guessthesong.domain.model.PlaylistSource
import org.mromichov.guessthesong.domain.model.RemotePlaylist
import org.mromichov.guessthesong.domain.model.Track
import javax.inject.Inject

class YandexMapper @Inject constructor() {

    fun toDomain(dto: YandexTrackDto): Track {
        val artistsFormatted = dto.artists.joinToString(", ") { it.name.trim() }.trim()
        val rawCover = dto.coverUri ?: dto.ogImage
        val coverUrl = rawCover?.let { formatCoverUrl(it) }

        return Track(
            title = dto.title,
            artist = artistsFormatted,
            coverUrl = coverUrl,
            durationMs = dto.durationMs
        )
    }

    fun toDomain(playlistId: String, dto: YandexWebPlaylistResponseDto): RemotePlaylist {
        val webPlaylist = dto.playlist
        val tracks = webPlaylist?.tracks.orEmpty().map { toDomain(it) }


        val playlistSource = PlaylistSource(PlaylistProvider.YANDEX_MUSIC, "")
        val playlistContent = PlaylistContent(
            title = webPlaylist?.title ?: "Yandex Music Playlist",
            tracks = tracks
        )

        return RemotePlaylist(
            source = playlistSource,
            content = playlistContent,
        )
    }

    fun toDomain(playlistId: String, dto: YandexApiPlaylistResponseDto): RemotePlaylist {
        val apiPlaylist = dto.result
        val tracks = apiPlaylist?.tracks.orEmpty()
            .mapNotNull { it.track }
            .map { toDomain(it) }

        val playlistSource = PlaylistSource(PlaylistProvider.YANDEX_MUSIC, "")
        val playlistContent = PlaylistContent(
            title = apiPlaylist?.title ?: "Yandex Music Playlist",
            tracks = tracks
        )

        return RemotePlaylist(
            source = playlistSource,
            content = playlistContent,
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
