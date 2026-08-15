package org.mromichov.guessthesong.data.remote.yandex

import javax.inject.Inject

sealed interface YandexPlaylistTarget {
    data class UserPlaylist(val owner: String, val kind: String) : YandexPlaylistTarget
    data class UuidPlaylist(val uuid: String) : YandexPlaylistTarget
}

class YandexPlaylistUrlParser @Inject constructor() {

    private val userPlaylistRegex = """.*music\.yandex\.[a-z]+/users/([^/]+)/playlists/(\d+).*""".toRegex()
    private val uuidPlaylistRegex = """.*music\.yandex\.[a-z]+/playlists/([a-zA-Z0-9-]+).*""".toRegex()

    fun parse(url: String): YandexPlaylistTarget? {
        val cleanUrl = url.trim().substringBefore("?")

        userPlaylistRegex.find(cleanUrl)?.let {
            return YandexPlaylistTarget.UserPlaylist(
                owner = it.groupValues[1],
                kind = it.groupValues[2]
            )
        }

        uuidPlaylistRegex.find(cleanUrl)?.let {
            return YandexPlaylistTarget.UuidPlaylist(
                uuid = it.groupValues[1]
            )
        }

        return null
    }
}
