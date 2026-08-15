package org.mromichov.guessthesong.data.remote.itunes

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

class ItunesApi @Inject constructor(private val client: HttpClient) {
    suspend fun searchTrackPreview(query: String): ItunesResponseDto =
        client.get("https://itunes.apple.com/search") {
            parameter("term", query)
            parameter("media", "music")
            parameter("limit", "1")
        }.body()
}