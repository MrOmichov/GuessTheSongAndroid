package org.mromichov.guessthesong.data.remote.yandex

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexApiPlaylistResponseDto
import javax.inject.Inject

class YandexApi @Inject constructor(
    private val client: HttpClient,
) {
    companion object {
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    suspend fun getUserPlaylist(owner: String, kind: String): YandexApiPlaylistResponseDto =
        client.get("https://api.music.yandex.net/users/$owner/playlists/$kind") {
            header(HttpHeaders.UserAgent, DEFAULT_USER_AGENT)
        }.body()

    suspend fun getUuidPlaylist(uuid: String): YandexApiPlaylistResponseDto =
        client.get("https://api.music.yandex.net/playlist/$uuid") {
            header(HttpHeaders.UserAgent, DEFAULT_USER_AGENT)
        }.body()
}