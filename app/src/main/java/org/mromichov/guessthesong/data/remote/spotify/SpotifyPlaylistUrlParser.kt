package org.mromichov.guessthesong.data.remote.spotify

import javax.inject.Inject

sealed interface SpotifyPlaylistTarget {
    data class Playlist(val id: String) : SpotifyPlaylistTarget
}

class SpotifyPlaylistUrlParser @Inject constructor() {

    private val playlistUrlRegex = """.*(?:^|//)open\.spotify\.com/(?:[a-zA-Z]{2,}(?:-[a-zA-Z]{2,})?/)?playlist/([a-zA-Z0-9]+).*""".toRegex(RegexOption.IGNORE_CASE)
    private val playlistUriRegex = """^spotify:playlist:([a-zA-Z0-9]+)$""".toRegex(RegexOption.IGNORE_CASE)

    fun parse(url: String): SpotifyPlaylistTarget? {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return null

        playlistUriRegex.find(cleanUrl)?.let {
            return SpotifyPlaylistTarget.Playlist(id = it.groupValues[1])
        }

        playlistUrlRegex.find(cleanUrl)?.let {
            return SpotifyPlaylistTarget.Playlist(id = it.groupValues[1])
        }

        return null
    }
}
