package org.mromichov.guessthesong.data.remote.yandex

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class YandexApi @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun getPlaylistByUuid(uuid: String): YandexPlaylistDto =
        client.get("https://api.music.yandex.net/playlist/$uuid").body()
}